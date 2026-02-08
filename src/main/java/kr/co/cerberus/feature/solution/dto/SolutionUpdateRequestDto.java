package kr.co.cerberus.feature.solution.dto;

import kr.co.cerberus.global.jsonb.FileInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SolutionUpdateRequestDto(
    @NotNull Long solutionId,
    @NotBlank String title,
    String description,
    String subject,
    List<FileInfo> solutionFiles
) {}
