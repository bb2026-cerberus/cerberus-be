package kr.co.cerberus.feature.mentor.dto;

import java.time.LocalDateTime;

// 멘토 홈 화면 - Q&A 요약 DTO
public record MentorQnaSummaryDto(
    Long qnaId,
    Long menteeId,
    String menteeName,
    String title,
    String status,
    LocalDateTime createDatetime
) {}