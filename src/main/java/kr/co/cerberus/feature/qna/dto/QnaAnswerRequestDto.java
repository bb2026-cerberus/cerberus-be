package kr.co.cerberus.feature.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QnaAnswerRequestDto(
    @NotNull
    @Schema(description = "멘토 ID", example = "1") Long mentorId,
    @NotNull
    @Schema(description = "Q&A ID", example = "1") Long qnaId,
    @NotBlank
    @Schema(description = "답변 내용", example = "멘티가 질문한 내용에 대한 답변입니다.") String answerContent
) {}
