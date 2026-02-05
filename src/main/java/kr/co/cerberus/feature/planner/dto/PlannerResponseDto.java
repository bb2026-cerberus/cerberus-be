package kr.co.cerberus.feature.planner.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PlannerResponseDto {
	private Long plannerId;
	private Long menteeId;
	private LocalDate date;
	private String imageUrl;
	private String question;
	private String studyTime;
	private String comment;
}
