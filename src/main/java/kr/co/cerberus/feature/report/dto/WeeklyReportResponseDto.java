package kr.co.cerberus.feature.report.dto;

import lombok.Builder;
import kr.co.cerberus.global.jsonb.FileInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record WeeklyReportResponseDto(
    Long id,
    Long menteeId,
    Long mentorId,
    LocalDate reportDate,
    String summary,
    String overallEvaluation,
    String strengths,
    String improvements,
    LocalDateTime createDatetime,
    LocalDateTime updateDatetime
) {}
