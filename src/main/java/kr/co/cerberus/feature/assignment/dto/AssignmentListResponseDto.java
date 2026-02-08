package kr.co.cerberus.feature.assignment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AssignmentListResponseDto {
	private Long assignmentId;
	private String title;
	private String subject;
	private String solution;
	private LocalDate date;
	private boolean completed;
}
