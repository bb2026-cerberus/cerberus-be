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
import kr.co.cerberus.global.util.JsonbUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {

	private final TodoRepository todoRepository;
	private final FeedbackRepository feedbackRepository;
	private final GoalRepository goalRepository;

	public List<AssignmentListResponseDto> findAssignments(Long menteeId, LocalDate startDate, LocalDate endDate) {
		List<Todo> assignments;

		if (startDate == null) {
			assignments = todoRepository.findByMenteeIdAndTodoAssignYnAndDeleteYn(menteeId, "Y", "N");
		} else if (endDate == null) {
			assignments = todoRepository.findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYn(menteeId, startDate, "Y", "N");
		} else {
			assignments = todoRepository.findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYn(menteeId, startDate, endDate, "Y", "N");
		}

		return assignments.stream()
				.map(todo -> {
					String goalName = getGoalName(todo.getGoalId());
					return AssignmentListResponseDto.builder()
							.assignmentId(todo.getId())
							.title(todo.getTodoName())
							.subject(todo.getTodoSubjects())
							.goal(goalName)
							.date(todo.getTodoDate())
							.completed("Y".equals(todo.getTodoCompleteYn()))
							.build();
				})
				.toList();
	}

	public AssignmentDetailResponseDto findAssignmentDetail(Long assignmentId) {
		Todo todo = findAssignmentById(assignmentId);

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

		// TODO: 실제 파일 저장 로직 구현 (다중 파일 업로드)
		String imageUrl = "/files/temp-" + assignmentId;

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

		// TODO: 기존 파일 삭제 후 새 파일 저장 로직 구현
		String imageUrl = "/files/temp-updated-" + assignmentId;

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
