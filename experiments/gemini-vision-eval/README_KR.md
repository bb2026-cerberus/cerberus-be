# Gemini 비전 - 이미지 여러 장(멘티 업로드) 해석/피드백 + 응답 분석 스타터킷

이 ZIP은 **이미지만 넣고 명령어 1~2줄**로 아래를 자동 수행합니다.

1) 멘티가 올린 이미지(여러 장 가능) 전처리(대비/선명도 보정)  
2) Gemini 비전 모델로 **문제 요약/풀이 요약/오답 원인/피드백/다음 과제**를 JSON으로 생성  
3) 케이스별/전체 **응답 분석 요약(신뢰도, 품질 이슈, 질문 발생률)** 리포트 생성

---
## 0) 폴더 구조(사용자가 하는 것)
- `inputs/case01/images/` 안에 **이미지 파일을 넣기만** 하면 됩니다.
  - 여러 장 업로드면 여러 파일을 넣어주세요. (jpg/png/webp 가능)
- 케이스를 늘리려면:
  - `inputs/case02/images/`, `inputs/case03/images/` ... 식으로 폴더만 추가하면 됩니다.

---
## 1) 설치
### Windows PowerShell
```powershell
cd gemini_vision_multimage_eval
python -m venv .venv
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
# .env 파일 열어서 GEMINI_API_KEY를 채워주세요.
```

### macOS / Linux
```bash
cd gemini_vision_multimage_eval
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# .env 파일 열어서 GEMINI_API_KEY를 채워주세요.
```

---
## 2) 실행(딱 한 줄)
```bash
python run_all.py
```

- 모든 케이스( inputs 아래 caseXX )를 자동 처리합니다.
- 결과는 `outputs/`에 저장됩니다.

---
## 3) 결과물
- `outputs/case01/result.json` : 모델 구조화 응답(JSON)
- `outputs/case01/result.md`   : 사람이 보기 좋은 요약
- `outputs/case01/preprocessed/` : 전처리된 이미지들
- `outputs/summary_report.md` : 전체 케이스 분석 요약
- `outputs/summary_stats.json` : 전체 통계(신뢰도 평균, 품질 이슈 빈도 등)

---
## 4) 제출(리포트)용 템플릿
- `templates/report_template.md` 를 복사해서 숫자만 채워 넣으면 됩니다.

---
## 5) 자주 터지는 문제
- **API 키 오류**: `.env`에 GEMINI_API_KEY가 비어있는지 확인
- **이미지 인식이 흔들림**: 사진을 종이만 꽉 차게, 정면/밝게 촬영하면 크게 개선됩니다.
- **여러 장이 많음**: 너무 큰 이미지는 용량이 커져 실패할 수 있어요. (권장: 한 장 2~4MB 수준)

---
## 6) 모델 변경
기본은 `gemini-2.5-flash` 입니다. 더 강한 모델로 비교하려면:
```bash
python run_all.py --model gemini-2.5-pro
```

