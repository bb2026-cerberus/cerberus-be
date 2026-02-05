package kr.co.cerberus.feature.todo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TodoCreateRequestDto {
	private Long menteeId;
	private String subject;
	private String title;
	private String content;
	private LocalDate date;
}
