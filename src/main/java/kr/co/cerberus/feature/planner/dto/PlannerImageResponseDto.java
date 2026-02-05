package kr.co.cerberus.feature.planner.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlannerImageResponseDto {
	private Long plannerId;
	private String imageUrl;
}
