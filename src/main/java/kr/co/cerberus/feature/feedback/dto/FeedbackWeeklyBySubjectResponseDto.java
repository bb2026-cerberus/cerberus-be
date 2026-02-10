package kr.co.cerberus.feature.feedback.dto;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
public record FeedbackWeeklyBySubjectResponseDto(
    String weekInfo,
    LocalDate mondayDate,
    String summary,
    Map<String, List<FeedbackWeeklyBySubjectResponseDto.FeedbackDetailDto>> feedback
) {
    @Builder
    public record FeedbackDetailDto(
            Long todoId,
            Long menteeId,
            String todoSubjects,
            Long feedbackId,
            String content,
            String summary,
            String draftYn,
            String completeYn
    ) {}
}
