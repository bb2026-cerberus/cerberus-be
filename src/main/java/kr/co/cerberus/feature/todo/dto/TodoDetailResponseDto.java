package kr.co.cerberus.feature.todo.dto;

import kr.co.cerberus.global.jsonb.FileInfo;
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
	private String solution;
	private LocalDate date;
	private boolean completed;
	private String subject;
	private List<FileInfo> workbooks;
	private List<FileInfo> studyVerificationImages;
	private String feedback;
}
