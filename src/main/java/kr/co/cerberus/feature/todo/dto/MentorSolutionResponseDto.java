package kr.co.cerberus.feature.todo.dto;

public record MentorSolutionResponseDto(
        Long solutionId,
        String solutionContent,
        String fileName,
        String fileUrl
) {
}
