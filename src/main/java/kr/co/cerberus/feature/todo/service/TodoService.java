package kr.co.cerberus.feature.todo.service;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.solution.service.SolutionService;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TodoService {

	private final TodoRepository todoRepository;
	private final FeedbackRepository feedbackRepository;
	private final SolutionService solutionService;
	private final FileStorageService fileStorageService;

	public List<GroupedTodosResponseDto> findTodos(Long menteeId, LocalDate startDate, LocalDate endDate) { // 반환 타입 변경
		// TODO: 보안 - 현재 로그인한 사용자가 요청한 menteeId에 접근 권한이 있는지 검증 필요
		List<Todo> todos;

		if (startDate == null) {
			todos = todoRepository.findByMenteeIdAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeId, "N", "N", "N");
		} else if (endDate == null) {
			todos = todoRepository.findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeId, startDate, "N", "N", "N");
		} else {
			todos = todoRepository.findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeId, startDate, endDate, "N", "N", "N");
		}

		// N+1 최적화: 필요한 Solution ID들을 수집하여 한 번에 조회
		Set<Long> solutionIds = todos.stream()
				.map(Todo::getSolutionId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> solutionTitleMap = solutionService.getAllSolutionTitle(solutionIds);

		todos.sort(Comparator.comparing(Todo::getTodoDate).reversed());
		Map<LocalDate, List<TodoListResponseDto>> groupedTodos = todos.stream()
				.map(todo -> TodoListResponseDto.builder()
						.todoId(todo.getId())
						.title(todo.getTodoName())
						.subject(todo.getTodoSubjects())
						.solution(solutionTitleMap.get(todo.getSolutionId()))
						.date(todo.getTodoDate())
						.completed("Y".equals(todo.getTodoCompleteYn()))
						.build())
				.collect(Collectors.groupingBy(TodoListResponseDto::getDate, TreeMap::new, Collectors.toList()));

		return groupedTodos.entrySet().stream()
				.map(entry -> GroupedTodosResponseDto.builder()
						.date(entry.getKey())
						.todos(entry.getValue())
						.build())
				.toList();
	}

	public List<GroupedTodosResponseDto> findDraftTodos(Long menteeId) {
		// TODO: 보안 - 현재 로그인한 사용자가 요청한 menteeId에 접근 권한이 있는지 검증 필요
		List<Todo> todos = todoRepository.findByMenteeIdAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeId, "N", "N", "Y");

		// N+1 최적화: 필요한 Solution ID들을 수집하여 한 번에 조회
		Set<Long> solutionIds = todos.stream()
				.map(Todo::getSolutionId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> solutionTitleMap = solutionService.getAllSolutionTitle(solutionIds);

		todos.sort(Comparator.comparing(Todo::getTodoDate).reversed());
		Map<LocalDate, List<TodoListResponseDto>> groupedTodos = todos.stream()
				.map(todo -> TodoListResponseDto.builder()
						.todoId(todo.getId())
						.title(todo.getTodoName())
						.subject(todo.getTodoSubjects())
						.solution(solutionTitleMap.get(todo.getSolutionId()))
						.date(todo.getTodoDate())
						.completed("Y".equals(todo.getTodoCompleteYn()))
						.build())
				.collect(Collectors.groupingBy(TodoListResponseDto::getDate, TreeMap::new, Collectors.toList()));

		return groupedTodos.entrySet().stream()
				.map(entry -> GroupedTodosResponseDto.builder()
						.date(entry.getKey())
						.todos(entry.getValue())
						.build())
				.toList();
	}

	public List<GroupedTodosResponseDto> findTodosWeekly(Long menteeId, LocalDate mondayDate) { // 반환 타입 변경
		LocalDate startDate = mondayDate;
		LocalDate endDate = mondayDate.plusDays(6);
		return findTodos(menteeId, startDate, endDate);
	}

	public TodoDetailResponseDto findTodoDetail(Long todoId) {
		Todo todo = findById(todoId);
		String solutionTitle = solutionService.getSolutionTitleById(todo.getSolutionId());

		// solution의 solutionFile JSONB 파싱 (solution에 연결된 학습지)
		List<FileInfo> solutionWorkbooks = solutionService.parseSolutionFiles(todo.getSolutionId());

		// 피드백 JSONB 파싱
		String feedbackContent = feedbackRepository.findByTodoIdAndDeleteYn(todoId, "N")
				.map(feedback -> {
					FeedbackFileData data = JsonbUtils.fromJson(feedback.getFeedFile(), FeedbackFileData.class);
					return data != null ? data.getContent() : null;
				})
				.orElse(null);

		// todoFile JSONB 파싱
		TodoFileData todoFileData = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);

		// todoFileData의 verificationImages JSONB 파싱
		List<FileInfo> verificationImages = Collections.emptyList();
		if (todoFileData != null && todoFileData.getVerificationImages() != null) {
			verificationImages = todoFileData.getVerificationImages();
		}

		return TodoDetailResponseDto.builder()
				.todoId(todo.getId())
				.title(todo.getTodoName())
				.content(todo.getTodoNote())
				.solution(solutionTitle)
				.date(todo.getTodoDate())
				.completed("Y".equals(todo.getTodoCompleteYn()))
				.subject(todo.getTodoSubjects())
				.workbooks(solutionWorkbooks)
				.studyVerificationImages(verificationImages)
				.feedback(feedbackContent)
				.build();
	}

	@Transactional
	public TodoCreateResponseDto createTodo(TodoCreateRequestDto request) {
		Todo todo = Todo.builder()
				.menteeId(request.getMenteeId())
				.todoSubjects(request.getSubject().getDescription())
				.todoName(request.getTitle())
				.todoNote(request.getContent())
				.todoDate(request.getDate())
				.solutionId(request.getSolutionId())
				.todoAssignYn("N")
				.todoCompleteYn("N")
				.todoDraftYn("N")
				.build();

		Todo saved = todoRepository.save(todo);

		return TodoCreateResponseDto.builder()
				.todoId(saved.getId())
				.title(saved.getTodoName())
				.content(saved.getTodoNote())
				.subject(saved.getTodoSubjects())
				.solution(solutionService.getSolutionTitleById(saved.getSolutionId()))
				.date(saved.getTodoDate())
				.completed("Y".equals(saved.getTodoCompleteYn()))
				.build();
	}

	@Transactional
	public TodoCreateResponseDto createDraftTodo(TodoCreateRequestDto request) {
		Todo todo = Todo.builder()
				.menteeId(request.getMenteeId())
				.todoSubjects(request.getSubject().getDescription())
				.todoName(request.getTitle())
				.todoNote(request.getContent())
				.todoDate(request.getDate())
				.solutionId(request.getSolutionId())
				.todoAssignYn("N")
				.todoCompleteYn("N")
				.todoDraftYn("Y")
				.build();

		Todo saved = todoRepository.save(todo);

		return TodoCreateResponseDto.builder()
				.todoId(saved.getId())
				.title(saved.getTodoName())
				.content(saved.getTodoNote())
				.subject(saved.getTodoSubjects())
				.solution(solutionService.getSolutionTitleById(saved.getSolutionId()))
				.date(saved.getTodoDate())
				.completed("Y".equals(saved.getTodoCompleteYn()))
				.build();
	}

	@Transactional
	public void markComplete(Long todoId) {
		Todo todo = findById(todoId);
		todo.markComplete();
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
				? existing.updateVerificationImages(fileInfos)
				: TodoFileData.withVerificationImages(fileInfos);
		
		todo.updateTodoFile(JsonbUtils.toJson(updated));

		List<String> imageUrls = fileInfos.stream()
				.map(FileInfo::getFileUrl)
				.toList();

		return VerificationResponseDto.builder()
				.imageUrls(imageUrls)
				.build();
	}

	@Transactional
	public VerificationResponseDto updateVerification(Long todoId, List<MultipartFile> images) {
		Todo todo = findById(todoId);

		// 피드백 존재 시 인증사진 수정 불가
		Optional<Feedback> feedback = feedbackRepository.findByTodoIdAndDeleteYn(todoId, "N");
		if (feedback.isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER);
		}

		// 모든 새 파일 저장
		List<FileInfo> fileInfos = images.stream()
				.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "todos"), null))
				.toList();

		// todoFile JSONB 업데이트
		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImages(fileInfos)
				: TodoFileData.withVerificationImages(fileInfos);

		todo.updateTodoFile(JsonbUtils.toJson(updated));

		List<String> imageUrls = fileInfos.stream()
				.map(FileInfo::getFileUrl)
				.toList();

		return VerificationResponseDto.builder()
				.imageUrls(imageUrls)
				.build();
	}

	@Transactional
	public VerificationResponseDto deleteVerificationImage(Long todoId) {
		Todo todo = findById(todoId);

		// 피드백 존재 시 인증사진 삭제 불가
		Optional<Feedback> feedback = feedbackRepository.findByTodoIdAndDeleteYn(todoId, "N");
		if (feedback.isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER);
		}

		// todoFile JSONB 파싱 -> 인증 사진 URL을 null로 설정
		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImages(Collections.emptyList())
				: TodoFileData.withVerificationImages(Collections.emptyList());
		todo.updateTodoFile(JsonbUtils.toJson(updated));

		return VerificationResponseDto.builder()
				.imageUrls(null)
				.build();
	}

	private Todo findById(Long id) {
		Todo todo = todoRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if ("Y".equals(todo.getTodoAssignYn())) {
			throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		return todo;
	}

    @Transactional
    public void addTimerSession(Long todoId, TodoTimerSessionCreateRequestDto request) {
        Todo todo = findById(todoId);

        if (request.getEndAt().isBefore(request.getStartAt())) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "종료 시간이 시작 시간보다 빠릅니다.");
        }

        todo.addTimerSession(request.getStartAt(), request.getEndAt());
    }


}
