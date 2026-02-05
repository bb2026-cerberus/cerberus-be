package kr.co.cerberus.feature.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TodoListResponseDto {
	private Long todoId;
	private String title;
	private String subject;
	private LocalDate date;
	private boolean completed;
}
