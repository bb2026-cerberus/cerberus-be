package kr.co.cerberus.feature.assignment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AssignmentDetailResponseDto {
	private Long assignmentId;
	private String title;
	private String content;
	private String goal;
	private LocalDate date;
	private boolean completed;
	private String subject;
	private List<fileDto> workbooks;
	private List<fileDto> studyVerificationImages;
	private String feedback;

	@Getter
	@Builder
	public static class fileDto {
		private String fileName;
		private String fileUrl;
		private String description;
	}
}
