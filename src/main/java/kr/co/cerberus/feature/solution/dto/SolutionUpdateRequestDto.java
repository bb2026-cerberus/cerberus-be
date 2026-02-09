package kr.co.cerberus.feature.solution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolutionUpdateRequestDto(
    @NotNull Long solutionId,
    @NotBlank String subject,
    @NotBlank String content
) {}
