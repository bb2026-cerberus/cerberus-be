package kr.co.cerberus.feature.mentor.dto;

import java.util.List;

// 멘토 홈 화면 통합 조회 응답 DTO
public record MentorHomeResponseDto(
    List<MentorAssignmentSummaryDto> assignments,
    List<MentorFeedbackSummaryDto> feedbacks,
    List<MentorQnaSummaryDto> qnas
) {}
