package kr.co.cerberus.feature.mentor.dto;

import java.util.List;

public record MenteeDetailsResponseDto(
    Long menteeId,
    String menteeName,
    TodayStatus todayStatus,
    WeeklyAchievement weeklyAchievement,
    List<SubjectProgressDto> subjectAchievement,
    String weeklyFeedbackSummary
) {
    public record TodayStatus(
        int completedCount,
        int totalCount,
        List<String> unsubmittedTitles
    ) {}

    public record WeeklyAchievement(
        double mentorAssignmentRate, // 멘토 과제 달성률
        double generalTodoRate       // 일반 할 일 달성률
    ) {}
}
