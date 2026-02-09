package kr.co.cerberus.feature.feedback.dto;

import jakarta.validation.constraints.NotNull;

public record FeedbackSaveRequestDto(
    @NotNull Long todoId,
    @NotNull Long menteeId,
    @NotNull Long mentorId,
    String content,
    String summary
) {}