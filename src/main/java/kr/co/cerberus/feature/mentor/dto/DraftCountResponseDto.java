package kr.co.cerberus.feature.mentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// 임시저장 개수 응답 DTO
@Schema(description = "임시저장 개수 정보")
public record DraftCountResponseDto(
    @Schema(description = "과제 임시저장 개수")
    long assignmentDraftCount,
    
    @Schema(description = "피드백 임시저장 개수")
    long feedbackDraftCount,
    
    @Schema(description = "전체 임시저장 개수 합계")
    long totalDraftCount
) {
    public DraftCountResponseDto(long assignmentDraftCount, long feedbackDraftCount) {
        this(assignmentDraftCount, feedbackDraftCount, assignmentDraftCount + feedbackDraftCount);
    }
}