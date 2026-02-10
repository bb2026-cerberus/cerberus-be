package kr.co.cerberus.feature.assignment.dto;

import kr.co.cerberus.global.common.Subject;
import java.time.LocalDate;
import java.util.List;

public record MentorAssignmentCreateRequestDto(
        Long menteeId,
        List<LocalDate> dates,
        java.time.LocalTime scheduledTime,
        Subject subject,
        String title,
        String content,
        Long solutionId,
        Long goalId,
        Boolean isDraft
) {
}
