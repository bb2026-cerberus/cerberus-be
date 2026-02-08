package kr.co.cerberus.feature.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record WeeklyMenteeReportResponseDto(
        Long id,
        Long mentorId,
        LocalDate reportDate,
        String summary,
        String overallEvaluation,
        String strengths,
        String improvements,
        LocalDateTime createDatetime,
        LocalDateTime updateDatetime,
        int assignmentPercent,
        int todoPercent,
        Map<String, Integer> subjectPercents
) {}
