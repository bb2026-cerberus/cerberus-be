package kr.co.cerberus.feature.mentor.dto;

import kr.co.cerberus.feature.feedback.domain.FeedbackStatus;
import java.time.LocalDate;

// 멘토 홈 화면 - 피드백 요약 DTO
public record MentorFeedbackSummaryDto(
    Long feedbackId,
    Long menteeId,
    String menteeName,
    Long todoId,
    LocalDate feedDate,
    FeedbackStatus status
) {}
