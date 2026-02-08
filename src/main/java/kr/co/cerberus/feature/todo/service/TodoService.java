package kr.co.cerberus.feature.todo.service;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.dto.*;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.jsonb.FeedbackFileData;
import kr.co.cerberus.global.jsonb.FileInfo;
import kr.co.cerberus.global.jsonb.TodoFileData;
import kr.co.cerberus.global.util.FileStorageService;
import kr.co.cerberus.global.util.JsonbUtils;
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
public class TodoService {

	private final TodoRepository todoRepository;
	private final FeedbackRepository feedbackRepository;
	private final FileStorageService fileStorageService;

	public List<TodoListResponseDto> findTodos(Long menteeId, LocalDate startDate, LocalDate endDate) {
		// TODO: 보안 - 현재 로그인한 사용자가 요청한 menteeId에 접근 권한이 있는지 검증 필요
		List<Todo> todos;

		if (startDate == null) {
			todos = todoRepository.findByMenteeIdAndTodoAssignYnAndDeleteYn(menteeId, "N", "N");
		} else if (endDate == null) {
			todos = todoRepository.findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYn(menteeId, startDate, "N", "N");
		} else {
			todos = todoRepository.findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYn(menteeId, startDate, endDate, "N", "N");
		}

		return todos.stream()
				.map(todo -> TodoListResponseDto.builder()
						.todoId(todo.getId())
						.title(todo.getTodoName())
						.subject(todo.getTodoSubjects())
						.date(todo.getTodoDate())
						.completed("Y".equals(todo.getTodoCompleteYn()))
						.build())
				.toList();
	}

	public TodoDetailResponseDto findTodoDetail(Long todoId) {
		Todo todo = findById(todoId);

		// 피드백 JSONB 파싱
		String feedbackContent = feedbackRepository.findByTodoIdAndDeleteYn(todoId, "N")
				.map(feedback -> {
					FeedbackFileData data = JsonbUtils.fromJson(feedback.getFeedFile(), FeedbackFileData.class);
					return data != null ? data.getContent() : null;
				})
				.orElse(null);

		// todoFile JSONB 파싱
		TodoFileData todoFileData = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);

		List<TodoDetailResponseDto.FileDto> attachments = Collections.emptyList();
		String verificationImage = null;

		if (todoFileData != null) {
			if (todoFileData.getAttachments() != null) {
				attachments = todoFileData.getAttachments().stream()
						.map(file -> TodoDetailResponseDto.FileDto.builder()
								.fileName(file.getFileName())
								.fileUrl(file.getFileUrl())
								.build())
						.toList();
			}
			verificationImage = todoFileData.getVerificationImage();
		}

		return TodoDetailResponseDto.builder()
				.todoId(todo.getId())
				.title(todo.getTodoName())
				.content(todo.getTodoNote())
				.date(todo.getTodoDate())
				.completed("Y".equals(todo.getTodoCompleteYn()))
				.subject(todo.getTodoSubjects())
				.attachments(attachments)
				.studyVerificationImage(verificationImage)
				.feedback(feedbackContent)
				.build();
	}

	@Transactional
	public TodoCreateResponseDto createTodo(TodoCreateRequestDto request) {
		Todo todo = Todo.builder()
				.menteeId(request.getMenteeId())
				.todoSubjects(request.getSubject())
				.todoName(request.getTitle())
				.todoNote(request.getContent())
				.todoDate(request.getDate())
				.todoAssignYn("N")
				.todoCompleteYn("N")
				.build();

		Todo saved = todoRepository.save(todo);

		return TodoCreateResponseDto.builder()
				.todoId(saved.getId())
				.title(saved.getTodoName())
				.subject(saved.getTodoSubjects())
				.goal(saved.getTodoNote())
				.date(saved.getTodoDate())
				.completed(false)
				.build();
	}

	@Transactional
	public void toggleStatus(Long todoId) {
		Todo todo = findById(todoId);
		todo.toggleComplete();
	}

	@Transactional
	public VerificationResponseDto uploadVerification(Long todoId, List<MultipartFile> images) {
		Todo todo = findById(todoId);

		// 모든 파일을 저장하고 FileInfo 리스트 생성
		List<FileInfo> fileInfos = images.stream()
				.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "todos"), null))
				.toList();

		// todoFile JSONB에 인증 정보 업데이트
		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateFiles(fileInfos)
				: TodoFileData.withFiles(fileInfos);
		
		todo.updateTodoFile(JsonbUtils.toJson(updated));

		return VerificationResponseDto.builder()
				.imageUrl(fileInfos.isEmpty() ? null : fileInfos.get(0).getFileUrl())
				.build();
	}

	private Todo findById(Long id) {
		return todoRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
	}
}
