package kr.co.cerberus.feature.feedback.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SubjectFeedbackResponseDto {
	private String weeklyFeedback;
	private List<FeedbackItemDto> assignment;

	@Getter
	@Builder
	public static class FeedbackItemDto {
		private String type;
		private Long dataId;
		private String subject;
		private String title;
		private String feedbackSummary;
		private LocalDate date;
	}
}
