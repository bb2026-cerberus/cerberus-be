package kr.co.cerberus.feature.report.dto;

import kr.co.cerberus.global.jsonb.FileInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReportUpdateRequestDto(
    @NotNull Long reportId,
    @NotNull LocalDate reportDate,
    @NotBlank String summary,
    @NotBlank String overallEvaluation,
    @NotBlank String strengths,
    @NotBlank String improvements,
    List<FileInfo> reportFiles
) {}
