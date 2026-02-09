package kr.co.cerberus.feature.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Q&A 질문 등록 요청 DTO")
public record QnaCreateRequestDto(
    @Schema(description = "멘티 ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull Long menteeId,

    @Schema(description = "질문 내용", example = "이 문제 풀이 방법을 알려주세요.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String questionContent,

    @Schema(description = "질문 날짜", example = "2026-02-09", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull LocalDate date
) {}
