package kr.co.cerberus.feature.solution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolutionCreateRequestDto(
    @NotNull Long menteeId,
    @NotNull Long mentorId,
    @NotBlank String subject,
    @NotBlank String content
) {}
