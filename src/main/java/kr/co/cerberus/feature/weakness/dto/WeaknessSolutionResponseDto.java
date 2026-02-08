package kr.co.cerberus.feature.weakness.dto;

import kr.co.cerberus.global.jsonb.FileInfo;
import java.time.LocalDateTime;
import java.util.List;

public record WeaknessSolutionResponseDto(
    Long id,
    Long menteeId,
    Long mentorId,
    String subject,
    String weaknessDescription,
    String solutionContent,
    List<FileInfo> solutionFiles,
    LocalDateTime createDatetime,
    LocalDateTime updateDatetime
) {}
