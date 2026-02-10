package kr.co.cerberus.feature.qna.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.qna.dto.QnaAnswerRequestDto;
import kr.co.cerberus.feature.qna.dto.QnaResponseDto;
import kr.co.cerberus.feature.qna.service.QnaService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Mentor Q&A Management", description = "멘토의 Q&A 관리 API (멘티 질문, 멘토 답변)")
@RestController
@RequestMapping("/api/mentors/qnas")
@RequiredArgsConstructor
public class MentorQnaController {

    private final QnaService qnaService;

    @Operation(summary = "Q&A 답변 등록 (멘토용)", description = "멘토가 멘티의 Q&A 질문에 답변을 등록합니다.")
    @PutMapping("/answer")
    public ResponseEntity<CommonResponse<QnaResponseDto>> answerQna(@Valid @RequestBody QnaAnswerRequestDto requestDto) {
        QnaResponseDto response = qnaService.answerQna(requestDto);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "Q&A 상세 조회", description = "특정 Q&A의 상세 정보를 조회합니다.")
    @GetMapping("/{qnaId}")
    public ResponseEntity<CommonResponse<QnaResponseDto>> getQnaDetail(@PathVariable Long qnaId) {
        QnaResponseDto response = qnaService.getQnaDetail(qnaId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘토별 Q&A 목록 조회", description = "멘토가 답변해야 할 또는 답변한 모든 Q&A 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<List<QnaResponseDto>>> getQnasByMentorId(
            @Parameter(description = "멘토 ID", required = true, example = "1") @RequestParam(value = "mentorId") Long mentorId,
            @Parameter(description = "조회 날짜 (YYYY-MM-DD)", required = true, example = "2026-02-09") @RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<QnaResponseDto> response = qnaService.getQnaByMentorIdAndDate(mentorId, date);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "Q&A 삭제", description = "멘토가 등록한 Q&A를 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<CommonResponse<Void>> deleteQna(
            @Parameter(description = "Q&A ID", required = true, example = "1") @RequestParam(value = "qnaId") Long qnaId) {

        qnaService.deleteQna(qnaId);
        return ResponseEntity.ok(CommonResponse.of(null));
    }
}
