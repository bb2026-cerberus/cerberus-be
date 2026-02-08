package kr.co.cerberus.feature.mentor.dto;

import kr.co.cerberus.feature.qna.domain.QnaStatus;
import java.time.LocalDateTime;

// 멘토 홈 화면 - Q&A 요약 DTO
public record MentorQnaSummaryDto(
    Long qnaId,
    Long menteeId,
    String menteeName, // 멘티 이름 추가 (가정)
    String title,
    QnaStatus status,
    LocalDateTime createDatetime
) {}
