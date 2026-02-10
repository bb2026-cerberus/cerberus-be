package kr.co.cerberus.feature.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.cerberus.feature.feedback.dto.FeedbackDetailResponseDto;
import kr.co.cerberus.global.jsonb.FileInfo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "할일/과제 상세 응답 DTO")
public class TodoDetailResponseDto {
	@Schema(description = "할일 ID", example = "1")
	private Long todoId;
	@Schema(description = "제목", example = "알고리즘 문제 풀이")
	private String title;
	@Schema(description = "내용", example = "백준 1234번 문제 풀이 및 제출")
	private String content;
	@Schema(description = "보완점", example = "첨부 파일 또는 추가 설명")
	private String solution;
	@Schema(description = "보완점 ID", example = "1")
	private Long solutionId;
	@Schema(description = "날짜", example = "2024-01-01")
	private LocalDate date;
	@Schema(description = "시간", example = "14:30")
	private java.time.LocalTime scheduledTime;
	@Schema(description = "과목", example = "수학")
	private String subject;
	@Schema(description = "멘티 ID", example = "1")
	private Long menteeId;
	@Schema(description = "완료 여부", example = "true")
	private boolean completed;
	@Schema(description = "피드백 작성완료 여부", example = "false")
	private boolean feedbackCompleted;
	@Schema(description = "과제 여부 (Y/N)", example = "N")
	private String assignYn;
	@Schema(description = "임시저장 여부 (Y/N)", example = "N")
	private String draftYn;
	@Schema(description = "첨부된 학습지 파일 목록")
	private List<FileInfo> workbooks;
	@Schema(description = "첨부된 학습 인증 이미지 목록")
	private List<FileInfo> studyVerificationImages;
	@Schema(description = "피드백 정보")
	private FeedbackDetailResponseDto.FeedbackInfo feedback;
}