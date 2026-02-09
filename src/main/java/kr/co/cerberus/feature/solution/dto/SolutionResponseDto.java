package kr.co.cerberus.feature.solution.dto;

import kr.co.cerberus.global.jsonb.FileInfo;
import java.time.LocalDateTime;

public record SolutionResponseDto(
    Long id,
    Long menteeId,
    Long mentorId,
	String solutionContent,
    String subject,
    FileInfo solutionFile,
    LocalDateTime createDatetime,
    LocalDateTime updateDatetime
) {}