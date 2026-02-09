package kr.co.cerberus.feature.mentor.dto;

import java.util.List;

public record MentorHomeResponseDto(
    List<MentorAssignmentSummaryDto> assignments,
    List<MentorFeedbackSummaryDto> feedbacks,
    List<MentorQnaSummaryDto> qnas,
    List<MenteeManagementDto> menteeManagement // 멘티별 학습 관리 현황 추가
) {
    // 하위 호환성을 위한 생성자 (기존 코드 영향 최소화)
    public MentorHomeResponseDto(
            List<MentorAssignmentSummaryDto> assignments,
            List<MentorFeedbackSummaryDto> feedbacks,
            List<MentorQnaSummaryDto> qnas) {
        this(assignments, feedbacks, qnas, List.of());
    }
}