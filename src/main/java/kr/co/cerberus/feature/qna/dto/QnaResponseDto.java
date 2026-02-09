package kr.co.cerberus.feature.qna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.cerberus.global.jsonb.FileInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Q&A 응답 DTO")
public record QnaResponseDto(
    @Schema(description = "Q&A ID", example = "1")
    Long qnaId,

    @Schema(description = "멘티 ID", example = "2")
    Long menteeId,

    @Schema(description = "멘토 ID", example = "1")
    Long mentorId,

    @Schema(description = "질문 날짜", example = "2026-02-09")
    LocalDate qnaDate,

    @Schema(description = "질문 내용")
    String questionContent,

    @Schema(description = "답변 내용")
    String answerContent,

    @Schema(description = "첨부 파일 목록")
    List<FileInfo> qnaFiles,

    @Schema(description = "생성 일시")
    LocalDateTime createDatetime,

    @Schema(description = "수정 일시")
    LocalDateTime updateDatetime
) {}
