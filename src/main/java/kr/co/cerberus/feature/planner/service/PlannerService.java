package kr.co.cerberus.feature.planner.service;

import kr.co.cerberus.feature.planner.Planner;
import kr.co.cerberus.feature.planner.dto.PlannerImageResponseDto;
import kr.co.cerberus.feature.planner.dto.PlannerQuestionRequestDto;
import kr.co.cerberus.feature.planner.dto.PlannerResponseDto;
import kr.co.cerberus.feature.planner.repository.PlannerRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.jsonb.PlannerFileData;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlannerService {

	private final PlannerRepository plannerRepository;
	private final TodoRepository todoRepository;

	public PlannerResponseDto findPlanner(Long menteeId, LocalDate date) {
		Planner planner = plannerRepository.findByMenteeIdAndPlanDateAndDeleteYn(menteeId, date, "N")
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		// planFile JSONB 파싱
		PlannerFileData plannerData = JsonbUtils.fromJson(planner.getPlanFile(), PlannerFileData.class);

		String imageUrl = (plannerData != null) ? plannerData.getImageUrl() : null;
		String question = (plannerData != null) ? plannerData.getQuestion() : null;

		// 학습 시간 계산: 해당 날짜의 할일/과제 중 start_dt ~ end_dt가 있는 항목들의 총 학습 시간
		String studyTime = calculateStudyTime(menteeId, date);

		return PlannerResponseDto.builder()
				.plannerId(planner.getId())
				.menteeId(planner.getMenteeId())
				.date(planner.getPlanDate())
				.imageUrl(imageUrl)
				.question(question)
				.studyTime(studyTime)
				.comment(planner.getPlanFeedback())
				.build();
	}

	@Transactional
	public PlannerImageResponseDto uploadImage(Long menteeId, LocalDate date, List<MultipartFile> images) {
		Planner planner = getOrCreatePlanner(menteeId, date);

		// TODO: 실제 파일 저장 로직 구현 (다중 파일 업로드)
		String imageUrl = "/files/temp-planner-" + planner.getId();

		// planFile JSONB 업데이트 (기존 question 유지)
		PlannerFileData existing = JsonbUtils.fromJson(planner.getPlanFile(), PlannerFileData.class);
		PlannerFileData updated = (existing != null)
				? existing.withImageUrl(imageUrl)
				: PlannerFileData.builder().imageUrl(imageUrl).build();
		planner.updatePlanFile(JsonbUtils.toJson(updated));

		return PlannerImageResponseDto.builder()
				.plannerId(planner.getId())
				.imageUrl(imageUrl)
				.build();
	}

	@Transactional
	public void registerQuestion(PlannerQuestionRequestDto request) {
		Planner planner = getOrCreatePlanner(request.getMenteeId(), request.getDate());

		// planFile JSONB 업데이트 (기존 imageUrl 유지)
		PlannerFileData existing = JsonbUtils.fromJson(planner.getPlanFile(), PlannerFileData.class);
		PlannerFileData updated = (existing != null)
				? existing.withQuestion(request.getQuestion())
				: PlannerFileData.builder().question(request.getQuestion()).build();
		planner.updatePlanFile(JsonbUtils.toJson(updated));
	}

	private Planner getOrCreatePlanner(Long menteeId, LocalDate date) {
		return plannerRepository.findByMenteeIdAndPlanDateAndDeleteYn(menteeId, date, "N")
				.orElseGet(() -> {
					Planner newPlanner = Planner.builder()
							.menteeId(menteeId)
							.planDate(date)
							.build();
					return plannerRepository.save(newPlanner);
				});
	}

	private String calculateStudyTime(Long menteeId, LocalDate date) {
		// 해당 날짜의 모든 할일+과제에서 start_dt ~ end_dt 기반 학습 시간 합산
		List<Todo> todosForDate = todoRepository.findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYn(menteeId, date, "N", "N");
		List<Todo> assignmentsForDate = todoRepository.findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYn(menteeId, date, "Y", "N");

		long totalSeconds = 0;

		for (Todo todo : todosForDate) {
			totalSeconds += calculateDuration(todo);
		}
		for (Todo todo : assignmentsForDate) {
			totalSeconds += calculateDuration(todo);
		}

		if (totalSeconds == 0) return null;

		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		return String.format("%d:%02d:%02d", hours, minutes, seconds);
	}

	private long calculateDuration(Todo todo) {
		if (todo.getTodoStartDt() == null || todo.getTodoEndDt() == null) return 0;
		Duration duration = Duration.between(todo.getTodoStartDt(), todo.getTodoEndDt());
		return Math.max(0, duration.getSeconds());
	}
}
