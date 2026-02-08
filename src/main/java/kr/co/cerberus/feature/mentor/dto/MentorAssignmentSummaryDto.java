package kr.co.cerberus.feature.mentor.dto;

import kr.co.cerberus.feature.assignment.domain.AssignmentStatus;
import java.time.LocalDate;

// 멘토 홈 화면 - 과제 요약 DTO
public record MentorAssignmentSummaryDto(
    Long assignmentId,
    Long menteeId,
    String menteeName, // 멘티 이름 추가 (가정)
    String title,
    LocalDate todoDate,
    AssignmentStatus status
) {}
