package kr.co.cerberus.feature.qna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QnaAnswerRequestDto(
    @NotNull Long qnaId,
    @NotBlank String answerContent
) {}
