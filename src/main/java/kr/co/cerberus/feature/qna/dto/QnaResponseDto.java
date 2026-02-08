package kr.co.cerberus.feature.qna.dto;

import kr.co.cerberus.global.jsonb.FileInfo;
import java.time.LocalDateTime;
import java.util.List;

public record QnaResponseDto(
    Long qnaId,
    Long menteeId,
    Long mentorId,
    String title,
    String questionContent,
    String answerContent,
    List<FileInfo> qnaFiles,
    LocalDateTime createDatetime,
    LocalDateTime updateDatetime
) {}