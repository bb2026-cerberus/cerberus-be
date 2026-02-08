package kr.co.cerberus.feature.mentor.dto;

// 과목별 진행률 DTO
public record SubjectProgressDto(
    String subject, // 과목명
    double progress // 진행률
) {}
