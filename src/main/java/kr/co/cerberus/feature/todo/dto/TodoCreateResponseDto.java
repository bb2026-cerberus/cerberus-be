package kr.co.cerberus.feature.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TodoCreateResponseDto {
	private Long todoId;
	private String title;
	private String content;
	private String subject;
	private String solution;
	private LocalDate date;
	private boolean completed;
}
