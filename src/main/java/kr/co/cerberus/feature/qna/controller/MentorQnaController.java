package kr.co.cerberus.feature.qna.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.qna.dto.QnaAnswerRequestDto;
import kr.co.cerberus.feature.qna.dto.QnaCreateRequestDto;
import kr.co.cerberus.feature.qna.dto.QnaResponseDto;
import kr.co.cerberus.feature.qna.dto.QnaUpdateRequestDto;
import kr.co.cerberus.feature.qna.service.QnaService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Mentor Q&A Management", description = "멘토의 Q&A 관리 API (멘티 질문, 멘토 답변)")
@RestController
@RequestMapping("/api/mentors/qnas")
@RequiredArgsConstructor
public class MentorQnaController {

    private final QnaService qnaService;

    @Operation(summary = "Q&A 질문 등록 (멘티용)", description = "멘티가 Q&A 질문을 등록합니다. {userId}와 {userRole}은 인증된 사용자의 ID와 역할입니다.")
    @PostMapping("/{userId}/{userRole}")
    public ResponseEntity<CommonResponse<QnaResponseDto>> createQna(
            @PathVariable Long userId,
            @PathVariable String userRole,
            @Valid @RequestBody QnaCreateRequestDto requestDto) {
        QnaResponseDto response = qnaService.createQna(userId, Role.valueOf(userRole), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.of(response));
    }

    @Operation(summary = "Q&A 질문 수정 (멘티용)", description = "멘티가 등록한 Q&A 질문을 수정합니다. 답변이 달리기 전까지만 가능합니다. {userId}와 {userRole}은 인증된 사용자의 ID와 역할입니다.")
    @PutMapping("/{userId}/{userRole}")
    public ResponseEntity<CommonResponse<QnaResponseDto>> updateQna(
            @PathVariable Long userId,
            @PathVariable String userRole,
            @Valid @RequestBody QnaUpdateRequestDto requestDto) {
        QnaResponseDto response = qnaService.updateQna(userId, Role.valueOf(userRole), requestDto);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "Q&A 답변 등록 (멘토용)", description = "멘토가 멘티의 Q&A 질문에 답변을 등록합니다. {userId}와 {userRole}은 인증된 사용자의 ID와 역할입니다.")
    @PutMapping("/answer/{userId}/{userRole}")
    public ResponseEntity<CommonResponse<QnaResponseDto>> answerQna(
            @PathVariable Long userId,
            @PathVariable String userRole,
            @Valid @RequestBody QnaAnswerRequestDto requestDto) {
        QnaResponseDto response = qnaService.answerQna(userId, Role.valueOf(userRole), requestDto);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "Q&A 삭제 (비활성화)", description = "Q&A를 비활성화합니다. {userId}와 {userRole}은 인증된 사용자의 ID와 역할입니다.")
    @DeleteMapping("/{userId}/{userRole}/{qnaId}")
    public ResponseEntity<CommonResponse<Void>> deleteQna(
            @PathVariable Long userId,
            @PathVariable String userRole,
            @PathVariable Long qnaId) {
        qnaService.deleteQna(userId, Role.valueOf(userRole), qnaId);
        return ResponseEntity.ok(CommonResponse.of(null));
    }

    @Operation(summary = "Q&A 상세 조회", description = "특정 Q&A의 상세 정보를 조회합니다.")
    @GetMapping("/{qnaId}")
    public ResponseEntity<CommonResponse<QnaResponseDto>> getQnaDetail(@PathVariable Long qnaId) {
        QnaResponseDto response = qnaService.getQnaDetail(qnaId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘토별 Q&A 목록 조회", description = "멘토가 답변해야 할 또는 답변한 모든 Q&A 목록을 조회합니다.")
    @GetMapping("/by-mentor/{mentorId}/{userRole}")
    public ResponseEntity<CommonResponse<List<QnaResponseDto>>> getQnasByMentorId(
            @PathVariable Long mentorId,
            @PathVariable String userRole) {
        List<QnaResponseDto> response = qnaService.getQnasByMentorId(mentorId, Role.valueOf(userRole));
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘티별 Q&A 목록 조회 (멘티용)", description = "멘티가 등록한 모든 Q&A 목록을 조회합니다.")
    @GetMapping("/by-mentee/{menteeId}/{userRole}")
    public ResponseEntity<CommonResponse<List<QnaResponseDto>>> getQnasByMenteeId(
            @PathVariable Long menteeId,
            @PathVariable String userRole) {
        List<QnaResponseDto> response = qnaService.getQnasByMenteeId(menteeId, Role.valueOf(userRole));
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
