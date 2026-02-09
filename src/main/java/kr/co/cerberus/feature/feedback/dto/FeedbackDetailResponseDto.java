package kr.co.cerberus.feature.feedback.dto;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

@Builder
public record FeedbackDetailResponseDto(
    Long todoId,
    String todoName,
    String todoNote,
    String todoSubjects,
    LocalDate todoDate,
    List<String> verificationImages,
    FeedbackInfo feedback
) {
    @Builder
    public record FeedbackInfo(
        Long feedbackId,
        String content,
        String summary,
        String draftYn,
        String completeYn
    ) {}
}
