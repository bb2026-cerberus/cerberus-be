package kr.co.cerberus.feature.assignment.service;

import kr.co.cerberus.feature.assignment.dto.AssignmentDetailResponseDto;
import kr.co.cerberus.feature.assignment.dto.AssignmentListResponseDto;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.goal.Goal;
import kr.co.cerberus.feature.goal.repository.GoalRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.dto.VerificationResponseDto;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.jsonb.FeedbackFileData;
import kr.co.cerberus.global.jsonb.FileInfo;
import kr.co.cerberus.global.jsonb.TodoFileData;
import kr.co.cerberus.global.util.FileStorageService;
import kr.co.cerberus.global.util.JsonbUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {

	private final TodoRepository todoRepository;
	private final FeedbackRepository feedbackRepository;
	private final GoalRepository goalRepository;
	private final FileStorageService fileStorageService;

	public List<AssignmentListResponseDto> findAssignments(Long menteeId, LocalDate startDate, LocalDate endDate) {
		List<Todo> assignments;

		if (startDate == null) {
			assignments = todoRepository.findByMenteeIdAndTodoAssignYnAndDeleteYn(menteeId, "Y", "N");
		} else if (endDate == null) {
			assignments = todoRepository.findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYn(menteeId, startDate, "Y", "N");
		} else {
			assignments = todoRepository.findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYn(menteeId, startDate, endDate, "Y", "N");
		}

		// N+1 최적화: 필요한 Goal ID들을 수집하여 한 번에 조회
		Set<Long> goalIds = assignments.stream()
				.map(Todo::getGoalId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> goalNameMap = goalRepository.findAllById(goalIds).stream()
				.collect(Collectors.toMap(Goal::getId, Goal::getGoalName));

		return assignments.stream()
				.map(todo -> AssignmentListResponseDto.builder()
							.assignmentId(todo.getId())
							.title(todo.getTodoName())
							.subject(todo.getTodoSubjects())
							.goal(goalNameMap.get(todo.getGoalId()))
							.date(todo.getTodoDate())
							.completed("Y".equals(todo.getTodoCompleteYn()))
							.build())
				.toList();
	}

	public AssignmentDetailResponseDto findAssignmentDetail(Long assignmentId) {
		Todo todo = findAssignmentById(assignmentId);

		// TODO: 보안 - 요청한 사용자가 해당 과제의 소유자인지(menteeId 일치 여부) 확인 로직 추가
		String goalName = getGoalName(todo.getGoalId());

		// goal의 goalFile JSONB 파싱 -> workbook 리스트
		List<AssignmentDetailResponseDto.WorkbookDto> workbooks = parseGoalFiles(todo.getGoalId());

		// 피드백 JSONB 파싱
		String feedbackContent = feedbackRepository.findByTodoIdAndDeleteYn(assignmentId, "N")
				.map(feedback -> {
					FeedbackFileData data = JsonbUtils.fromJson(feedback.getFeedFile(), FeedbackFileData.class);
					return data != null ? data.getContent() : null;
				})
				.orElse(null);

		// todoFile JSONB 파싱 -> 인증 사진 URL
		TodoFileData todoFileData = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		String verificationImage = (todoFileData != null) ? todoFileData.getVerificationImage() : null;

		return AssignmentDetailResponseDto.builder()
				.assignmentId(todo.getId())
				.title(todo.getTodoName())
				.content(todo.getTodoNote())
				.goal(goalName)
				.date(todo.getTodoDate())
				.completed("Y".equals(todo.getTodoCompleteYn()))
				.subject(todo.getTodoSubjects())
				.workbook(workbooks)
				.studyVerificationImage(verificationImage)
				.feedback(feedbackContent)
				.build();
	}

	@Transactional
	public VerificationResponseDto uploadVerification(Long assignmentId, List<MultipartFile> images) {
		Todo todo = findAssignmentById(assignmentId);

		// 실제 파일 저장 (첫 번째 파일만 대표 이미지로 사용하거나 구조에 맞게 조정)
		String imageUrl = images.isEmpty() ? null : fileStorageService.storeFile(images.get(0), "assignments");

		// todoFile JSONB에 인증 사진 URL 저장
		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImage(imageUrl)
				: TodoFileData.withVerification(imageUrl);
		todo.updateTodoFile(JsonbUtils.toJson(updated));

		// 인증 사진 업로드 시 자동으로 완료 표시 전환
		todo.markComplete();

		return VerificationResponseDto.builder()
				.imageUrl(imageUrl)
				.build();
	}

	@Transactional
	public VerificationResponseDto updateVerification(Long assignmentId, List<MultipartFile> images) {
		Todo todo = findAssignmentById(assignmentId);

		// 새 파일 저장
		String imageUrl = images.isEmpty() ? null : fileStorageService.storeFile(images.get(0), "assignments");

		// todoFile JSONB 업데이트
		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImage(imageUrl)
				: TodoFileData.withVerification(imageUrl);
		todo.updateTodoFile(JsonbUtils.toJson(updated));

		return VerificationResponseDto.builder()
				.imageUrl(imageUrl)
				.build();
	}

	private Todo findAssignmentById(Long id) {
		Todo todo = todoRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if (!"Y".equals(todo.getTodoAssignYn())) {
			throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		return todo;
	}

	private String getGoalName(Long goalId) {
		if (goalId == null) return null;
		return goalRepository.findByIdAndDeleteYn(goalId, "N")
				.map(Goal::getGoalName)
				.orElse(null);
	}

	private List<AssignmentDetailResponseDto.WorkbookDto> parseGoalFiles(Long goalId) {
		if (goalId == null) return Collections.emptyList();

		return goalRepository.findByIdAndDeleteYn(goalId, "N")
				.map(goal -> {
					List<FileInfo> files = JsonbUtils.fromJson(
							goal.getGoalFile(), new TypeReference<List<FileInfo>>() {});
					if (files == null) return Collections.<AssignmentDetailResponseDto.WorkbookDto>emptyList();

					return files.stream()
							.map(file -> AssignmentDetailResponseDto.WorkbookDto.builder()
									.fileName(file.getFileName())
									.fileUrl(file.getFileUrl())
									.description(file.getDescription())
									.build())
							.toList();
				})
				.orElse(Collections.emptyList());
	}
}
