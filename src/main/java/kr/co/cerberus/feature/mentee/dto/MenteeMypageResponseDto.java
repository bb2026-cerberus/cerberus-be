package kr.co.cerberus.feature.mentee.dto;

import kr.co.cerberus.feature.mentor.dto.SubjectProgressDto;

import java.util.List;

public record MenteeMypageResponseDto(
    Long menteeId,
    String menteeName,
    WeeklyAchievement weeklyAchievement,
    List<SubjectProgressDto> subjectAchievement
) {
    public record WeeklyAchievement(
        double mentorAssignmentRate, // 멘토 과제 달성률
        double generalTodoRate       // 일반 할 일 달성률
    ) {}
}
