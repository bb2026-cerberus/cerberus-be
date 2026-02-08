package kr.co.cerberus.feature.qna.dto;

import kr.co.cerberus.feature.qna.domain.QnaStatus;
import kr.co.cerberus.global.jsonb.FileInfo;
import java.time.LocalDateTime;
import java.util.List;

public record QnaResponseDto(
    Long id,
    Long menteeId,
    Long mentorId,
    Long relatedEntityId,
    String relatedEntityType,
    String title,
    String questionContent,
    String answerContent,
    List<FileInfo> qnaFiles,
    QnaStatus status,
    LocalDateTime createDatetime,
    LocalDateTime updateDatetime
) {}
