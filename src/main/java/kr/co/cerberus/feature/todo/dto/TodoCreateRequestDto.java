package kr.co.cerberus.feature.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.cerberus.global.common.Subject;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TodoCreateRequestDto {

	@Schema(description = "멘티 ID", example = "2")
	private Long menteeId;

	@Schema(description = "과목", allowableValues = {"KOREAN", "ENGLISH", "MATH"}, example = "KOREAN")
	private Subject subject;

	@Schema(description = "할일 제목", example = "수학 문제집 20p 풀기")
	private String title;

	@Schema(description = "할일 내용", example = "수학의 정석 미적분 20페이지부터 25페이지까지 풀고 채점하기")
	private String content;

	@Schema(description = "할일 날짜 (YYYY-MM-DD)", example = "2026-02-08")
	private LocalDate date;

	@Schema(description = "할일 시간 (HH:mm)", example = "14:30")
	private java.time.LocalTime scheduledTime;

	@Schema(description = "솔루션 ID", example = "1", nullable = true)
	private Long solutionId;
}
