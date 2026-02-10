package kr.co.cerberus.feature.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.cerberus.global.common.Subject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "할일/과제 저장 요청 DTO")
public class TodoSaveRequestDto {

	@Schema(description = "멘티 ID", example = "2")
	private Long menteeId;

	@Schema(description = "과목", allowableValues = {"KOREAN", "ENGLISH", "MATH"}, example = "KOREAN")
	private Subject subject;

	@Schema(description = "제목", example = "수학 문제집 20p 풀기")
	private String title;

	@Schema(description = "내용", example = "수학의 정석 미적분 20페이지부터 25페이지까지 풀고 채점하기")
	private String content;

	@Schema(description = "날짜 목록 (할일은 보통 1개, 과제는 여러 개 가능)", example = "["2026-02-08"]")
	private List<LocalDate> dates;

	@Schema(description = "시간 (HH:mm)", example = "14:30")
	private LocalTime scheduledTime;

	@Schema(description = "솔루션 ID", example = "1", nullable = true)
	private Long solutionId;

	@Schema(description = "과제 여부 (Y/N)", example = "N")
	private String assignYn;

	@Schema(description = "임시저장 여부 (Y/N)", example = "N")
	private String draftYn;
}
