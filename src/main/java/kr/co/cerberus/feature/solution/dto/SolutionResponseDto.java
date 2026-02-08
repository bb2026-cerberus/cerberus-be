package kr.co.cerberus.feature.solution.dto;

import kr.co.cerberus.global.jsonb.FileInfo;
import java.time.LocalDateTime;
import java.util.List;

public record SolutionResponseDto(
    Long id,
    Long mentorId,
    String title,
    String description,
    String subject,
    List<FileInfo> solutionFiles,
    LocalDateTime createDatetime,
    LocalDateTime updateDatetime
) {}
