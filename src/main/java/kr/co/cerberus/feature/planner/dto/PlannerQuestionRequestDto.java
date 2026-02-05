package kr.co.cerberus.feature.planner.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class PlannerQuestionRequestDto {
	private Long menteeId;
	private LocalDate date;
	private String question;
}
