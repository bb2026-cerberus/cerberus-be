import argparse
import glob
import json
import os
import pathlib
from collections import Counter
from datetime import datetime
from typing import List

from dotenv import load_dotenv
from PIL import Image, ImageOps, ImageEnhance
from pydantic import BaseModel
from rich import print
from rich.console import Console

from google import genai
from google.genai import types


# ---------------------------
# Output schema (structured JSON)
# ---------------------------
class Mistake(BaseModel):
    where: str
    why: str
    fix: str

class Result(BaseModel):
    summary: str
    extracted_problem: str
    student_work_summary: str
    mistakes: List[Mistake]
    feedback_actions: List[str]
    next_practice: List[str]
    questions_to_student: List[str]
    image_quality_issues: List[str]
    confidence: float


DEFAULT_PROMPT = """    너는 '학습지/오답노트' 사진을 보고 멘티에게 피드백을 주는 튜터다.
입력은 멘티가 업로드한 이미지(여러 장 가능)이다.

반드시 지켜:
- 사진에 없는 내용을 지어내지 마라(추측 금지).
- 글씨/수식이 애매하면 'questions_to_student'에 확인 질문을 적고, confidence를 낮춰라.
- 피드백은 '다음 행동' 중심으로 구체적으로 써라(어떤 성질/정리, 어떤 비교 방법, 어떤 실수 패턴).

출력은 JSON만. 아래 필드를 정확히 채워라:
- summary: 전체 한줄 요약
- extracted_problem: 문제 조건/질문 요약(보이는 범위에서)
- student_work_summary: 멘티 풀이 흐름 요약
- mistakes: [{where, why, fix}] (최소 1개)
- feedback_actions: 멘티가 바로 할 수 있는 행동 3~5개
- next_practice: 비슷한 유형을 연습하기 위한 미니 연습/과제 2~4개
- questions_to_student: 애매한 부분 확인 질문(없으면 빈 배열)
- image_quality_issues: (흐림/기울어짐/그림자/크롭 필요 등)
- confidence: 0~1
"""


def preprocess_image(inp_path: str, out_path: str) -> str:
    """Lightweight preprocessing to improve readability."""
    im = Image.open(inp_path).convert("RGB")
    g = im.convert("L")
    g = ImageOps.autocontrast(g, cutoff=0)
    g = ImageEnhance.Contrast(g).enhance(3.0)
    g = ImageEnhance.Brightness(g).enhance(0.85)
    g = ImageEnhance.Sharpness(g).enhance(2.0)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    g.save(out_path)
    return out_path


def load_images(paths: List[str]) -> List[Image.Image]:
    imgs = []
    for p in paths:
        imgs.append(Image.open(p))
    return imgs


def run_case(client: genai.Client, model: str, case_dir: pathlib.Path, out_dir: pathlib.Path, prompt: str, temperature: float) -> dict:
    case_name = case_dir.name
    img_dir = case_dir / "images"
    raw_paths = sorted([p for p in img_dir.glob("*") if p.suffix.lower() in [".png", ".jpg", ".jpeg", ".webp"]])

    if not raw_paths:
        raise FileNotFoundError(f"No images found in: {img_dir}")

    # Preprocess
    pre_dir = out_dir / case_name / "preprocessed"
    pre_dir.mkdir(parents=True, exist_ok=True)
    pre_paths = []
    for rp in raw_paths:
        outp = pre_dir / (rp.stem + ".png")
        pre_paths.append(preprocess_image(str(rp), str(outp)))

    imgs = load_images(pre_paths)

    # Call Gemini
    resp = client.models.generate_content(
        model=model,
        contents=[prompt, *imgs],
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=Result,
            temperature=temperature,
        )
    )

    data = resp.parsed.model_dump() if getattr(resp, "parsed", None) else json.loads(resp.text)

    # Save outputs
    case_out = out_dir / case_name
    case_out.mkdir(parents=True, exist_ok=True)
    (case_out / "result.json").write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    # Human-readable markdown
    md = []
    md.append(f"# {case_name} - Gemini 비전 피드백\n")
    md.append(f"- generated_at: {datetime.now().isoformat(timespec='seconds')}\n")
    md.append(f"- model: {model}\n")
    md.append(f"- confidence: {data.get('confidence')}\n")
    md.append("")
    md.append("## 요약")
    md.append(data.get("summary",""))
    md.append("")
    md.append("## 문제 요약(보이는 범위)")
    md.append(data.get("extracted_problem",""))
    md.append("")
    md.append("## 멘티 풀이 요약")
    md.append(data.get("student_work_summary",""))
    md.append("")
    md.append("## 실수/오답 포인트")
    for i, m in enumerate(data.get("mistakes", []), 1):
        md.append(f"### {i}. {m.get('where','')}")
        md.append(f"- 왜 문제인가: {m.get('why','')}")
        md.append(f"- 어떻게 고치나: {m.get('fix','')}")
        md.append("")
    md.append("## 다음 행동(체크리스트)")
    for a in data.get("feedback_actions", []):
        md.append(f"- {a}")
    md.append("")
    md.append("## 다음 연습(과제)")
    for a in data.get("next_practice", []):
        md.append(f"- {a}")
    md.append("")
    qs = data.get("questions_to_student", [])
    if qs:
        md.append("## 확인 질문(불확실)")
        for q in qs:
            md.append(f"- {q}")
        md.append("")
    issues = data.get("image_quality_issues", [])
    if issues:
        md.append("## 이미지 품질 이슈")
        for it in issues:
            md.append(f"- {it}")
        md.append("")
    (case_out / "result.md").write_text("\n".join(md), encoding="utf-8")

    meta = {
        "case": case_name,
        "model": model,
        "raw_images": [str(p) for p in raw_paths],
        "preprocessed_images": [str(p) for p in pre_paths],
        "temperature": temperature,
        "generated_at": datetime.now().isoformat(timespec="seconds"),
    }
    (case_out / "meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")

    return data


def summarize_all(out_dir: pathlib.Path, model: str) -> dict:
    # Collect results
    results = []
    for case_path in sorted(out_dir.glob("case*")):
        rj = case_path / "result.json"
        if rj.exists():
            results.append(json.loads(rj.read_text(encoding="utf-8")))

    if not results:
        return {}

    confs = [float(r.get("confidence", 0.0)) for r in results]
    q_counts = [len(r.get("questions_to_student", [])) for r in results]
    issue_counter = Counter()
    for r in results:
        for it in r.get("image_quality_issues", []):
            issue_counter[it.strip()] += 1

    summary = {
        "cases": len(results),
        "model": model,
        "avg_confidence": sum(confs)/len(confs) if confs else 0.0,
        "avg_questions_to_student": sum(q_counts)/len(q_counts) if q_counts else 0.0,
        "image_quality_issues_top": issue_counter.most_common(10),
    }

    # Write markdown report
    md = []
    md.append("# Gemini 비전 응답 분석 요약\n")
    md.append(f"- cases: {summary['cases']}")
    md.append(f"- model: {model}")
    md.append(f"- avg_confidence: {summary['avg_confidence']:.3f}")
    md.append(f"- avg_questions_to_student: {summary['avg_questions_to_student']:.2f}\n")
    md.append("## image_quality_issues Top")
    for k, v in summary["image_quality_issues_top"]:
        md.append(f"- {k}: {v}")
    md.append("\n## 해석/다음 액션")
    md.append("- avg_confidence가 낮거나 questions_to_student가 많으면: 이미지 품질(정면/밝기/크롭) 개선, 혹은 멘토 검수 플래그 적용")
    md.append("- image_quality_issues Top 항목에 맞춰 업로드 가이드를 문구로 고정하는 것을 추천")
    (out_dir / "summary_report.md").write_text("\n".join(md), encoding="utf-8")
    (out_dir / "summary_stats.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")

    return summary


def main():
    load_dotenv()
    ap = argparse.ArgumentParser()
    ap.add_argument("--input_dir", default="inputs", help="inputs/caseXX/images 구조")
    ap.add_argument("--output_dir", default="outputs")
    ap.add_argument("--model", default=os.getenv("GEMINI_MODEL", "gemini-2.5-flash"))
    ap.add_argument("--case", default="", help="특정 케이스만 실행하려면 case01 같은 이름")
    ap.add_argument("--temperature", type=float, default=0.0)
    ap.add_argument("--prompt_file", default="", help="커스텀 프롬프트 파일(txt/md)")
    args = ap.parse_args()

    if not os.getenv("GEMINI_API_KEY"):
        raise SystemExit("❌ GEMINI_API_KEY가 없습니다. .env 파일을 만들고 키를 넣어주세요. (.env.example 참고)")

    prompt = DEFAULT_PROMPT
    if args.prompt_file:
        prompt = pathlib.Path(args.prompt_file).read_text(encoding="utf-8")

    in_dir = pathlib.Path(args.input_dir)
    out_dir = pathlib.Path(args.output_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))
    console = Console()

    case_dirs = []
    if args.case:
        case_path = in_dir / args.case
        if not case_path.exists():
            raise SystemExit(f"❌ 케이스 폴더가 없습니다: {case_path}")
        case_dirs = [case_path]
    else:
        case_dirs = sorted([p for p in in_dir.glob("case*") if p.is_dir()])

    if not case_dirs:
        raise SystemExit(f"❌ 실행할 케이스가 없습니다. {in_dir}/case01/images/에 이미지를 넣어주세요.")

    console.print(f"[bold]Running {len(case_dirs)} case(s) with model={args.model}[/bold]")

    for cd in case_dirs:
        console.print(f"\n[cyan]=== {cd.name} ===[/cyan]")
        try:
            data = run_case(client, args.model, cd, out_dir, prompt, args.temperature)
            console.print(f"[green]saved: outputs/{cd.name}/result.json[/green]  (confidence={data.get('confidence')})")
        except Exception as e:
            console.print(f"[red]failed: {cd.name} -> {e}[/red]")

    summary = summarize_all(out_dir, args.model)
    if summary:
        console.print("\n[bold green]All done![/bold green]")
        console.print(f"- summary: outputs/summary_report.md")
        console.print(f"- stats: outputs/summary_stats.json")


if __name__ == "__main__":
    main()
