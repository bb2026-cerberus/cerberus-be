package kr.co.cerberus.feature.mentor.dto;

import java.util.List;

// 멘티별 진행률 통계 응답 DTO
public record MenteeProgressResponseDto(
    Long menteeId,
    String menteeName, // 멘티 이름 추가 (가정)
    double overallProgress, // 전체 진행률
    List<SubjectProgressDto> subjectProgressList // 과목별 진행률
) {}
