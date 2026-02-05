package kr.co.cerberus.feature.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TodoDetailResponseDto {
	private Long todoId;
	private String title;
	private String content;
	private LocalDate date;
	private boolean completed;
	private String subject;
	private List<FileDto> attachments;
	private String studyVerificationImage;
	private String feedback;

	@Getter
	@Builder
	public static class FileDto {
		private String fileName;
		private String fileUrl;
	}
}
