package kr.co.cerberus.feature.weakness.dto;

import kr.co.cerberus.global.jsonb.FileInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WeaknessSolutionUpdateRequestDto(
    @NotNull Long weaknessSolutionId,
    @NotBlank String subject,
    @NotBlank String weaknessDescription,
    @NotBlank String solutionContent,
    List<FileInfo> solutionFiles
) {}
