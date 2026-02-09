package kr.co.cerberus.feature.feedback.dto;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
public record FeedbackWeeklyResponseDto(
    String weekInfo,
    LocalDate mondayDate,
    Map<LocalDate, List<TodoWithFeedbackDto>> groupedItems
) {
    @Builder
    public record TodoWithFeedbackDto(
        Long todoId,
        Long menteeId,
        String menteeName,
        String todoName,
        String todoNote,
        String todoSubjects,
        String todoCompleteYn,
        String todoAssignYn,
        LocalDate todoDate,
        List<String> verificationImages,
        FeedbackDetailDto feedback
    ) {}

    @Builder
    public record FeedbackDetailDto(
        Long feedbackId,
        String content,
        String summary,
        String draftYn,
        String completeYn
    ) {}
}