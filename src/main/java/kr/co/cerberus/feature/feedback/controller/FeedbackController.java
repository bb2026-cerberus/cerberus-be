package kr.co.cerberus.feature.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.feedback.dto.SubjectFeedbackResponseDto;
import kr.co.cerberus.feature.feedback.service.FeedbackService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Feedback", description = "과목별 피드백 관련 API")
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

	private final FeedbackService feedbackService;

	@Operation(summary = "과목별 피드백 요약 조회", description = "endDate 기준 과거 일주일의 과제와 할일에 대한 피드백 요약 데이터를 조회합니다.")
	@GetMapping("/by-subject")
	public ResponseEntity<CommonResponse<SubjectFeedbackResponseDto>> getFeedbackBySubject(
			@RequestParam Long menteeId,
			@RequestParam String subject,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		SubjectFeedbackResponseDto response = feedbackService.findFeedbackBySubject(menteeId, subject, endDate);
		return ResponseEntity.ok(CommonResponse.of(response));
	}
}
