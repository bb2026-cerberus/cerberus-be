package kr.co.cerberus.feature.assignment.dto;

public record MentorSolutionResponseDto(
        Long solutionId,
        String solutionContent,
        String fileName,
        String fileUrl
) {
}