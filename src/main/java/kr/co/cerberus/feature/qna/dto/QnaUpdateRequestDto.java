package kr.co.cerberus.feature.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Q&A 질문 수정 요청 DTO")
public record QnaUpdateRequestDto(
    @Schema(description = "Q&A ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull Long qnaId,

    @Schema(description = "질문 내용", example = "수정된 질문 내용입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String questionContent
) {}
