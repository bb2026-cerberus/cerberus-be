package kr.co.cerberus.feature.mentor.dto;

// 임시저장 개수 응답 DTO
public record DraftCountResponseDto(
    long assignmentDraftCount,
    long feedbackDraftCount
) {}
