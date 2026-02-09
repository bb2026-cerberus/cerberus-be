package kr.co.cerberus.feature.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.feedback.dto.FeedbackWeeklyResponseDto;
import kr.co.cerberus.feature.feedback.dto.FeedbackDetailResponseDto;
import kr.co.cerberus.feature.feedback.dto.FeedbackSaveRequestDto;
import kr.co.cerberus.feature.feedback.service.FeedbackService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Feedback Management", description = "멘토의 피드백 관리 API")
@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "주간 피드백 목록 조회", description = "입력된 날짜가 포함된 주차의 모든 할 일과 작성된 피드백을 조회합니다.")
    @GetMapping("/weekly")
    public ResponseEntity<CommonResponse<FeedbackWeeklyResponseDto>> getWeeklyFeedbacks(
            @RequestParam Long mentorId,
            @RequestParam(required = false) Long menteeId,
            @RequestParam(defaultValue = "ASSIGNMENT") String type, // ASSIGNMENT, TODO
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        FeedbackWeeklyResponseDto response = feedbackService.getWeeklyFeedbacks(mentorId, menteeId, date, type);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "피드백 상세 조회", description = "특정 할 일(Todo)에 대한 상세 정보와 작성된 피드백을 함께 조회합니다.")
    @GetMapping("/{todoId}")
    public ResponseEntity<CommonResponse<FeedbackDetailResponseDto>> getFeedbackDetail(@PathVariable Long todoId) {
        FeedbackDetailResponseDto response = feedbackService.getFeedbackDetail(todoId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "임시저장 피드백 목록 조회", description = "멘토가 작성 중인 모든 임시저장 피드백 목록을 조회합니다.")
    @GetMapping("/drafts/{mentorId}")
    public ResponseEntity<CommonResponse<List<FeedbackWeeklyResponseDto.TodoWithFeedbackDto>>> getDraftFeedbacks(
            @PathVariable Long mentorId) {
        List<FeedbackWeeklyResponseDto.TodoWithFeedbackDto> response = feedbackService.getDraftFeedbacks(mentorId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "피드백 저장 및 수정", description = "피드백을 생성하거나 수정합니다. 임시저장(draftYn='Y')과 완료(completeYn='Y')를 구분합니다.")
    @PostMapping
    public ResponseEntity<CommonResponse<Void>> saveFeedback(
            @Valid @RequestBody FeedbackSaveRequestDto requestDto) {
        feedbackService.saveFeedback(requestDto);
        return ResponseEntity.ok(CommonResponse.of(null));
    }

    /**
     * 내부 트리거용 API
     * 실제 멘티 API에서 이미지가 업로드된 후 호출될 수 있습니다.
     */
    @Operation(summary = "[백그라운드] AI Vision 분석 트리거", description = "할 일의 이미지를 분석하여 피드백 초안을 백그라운드에서 생성합니다.")
    @PostMapping("/analyze/{todoId}")
    public ResponseEntity<CommonResponse<String>> triggerImageAnalysis(@PathVariable Long todoId) {
        feedbackService.analyzeTodoImagesAsync(todoId);
        return ResponseEntity.ok(CommonResponse.of("AI 분석이 백그라운드에서 시작되었습니다."));
    }
}