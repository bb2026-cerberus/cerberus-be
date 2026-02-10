package kr.co.cerberus.feature.mentor.dto;

import java.util.List;

public record MentorHomeResponseDto(
    List<MentorAssignmentSummaryDto> assignments,
    List<MentorFeedbackSummaryDto> feedbacks,
    List<MentorQnaSummaryDto> qnas,
    List<MenteeManagementDto> menteeManagement
) {}