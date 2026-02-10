package kr.co.cerberus.feature.todo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.cerberus.feature.todo.dto.MentorSolutionResponseDto;
import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.dto.FeedbackDetailResponseDto;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.solution.Solution;
import kr.co.cerberus.feature.solution.repository.SolutionRepository;
import kr.co.cerberus.feature.solution.service.SolutionService;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.dto.*;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.jsonb.FileInfo;
import kr.co.cerberus.global.jsonb.TodoFileData;
import kr.co.cerberus.global.util.FileStorageService;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TodoService {

	private final TodoRepository todoRepository;
	private final FeedbackRepository feedbackRepository;
	private final SolutionRepository solutionRepository;
	private final SolutionService solutionService;
	private final FileStorageService fileStorageService;
	private final MemberRepository memberRepository;

	/**
	 * 할일/과제 목록 조회
	 */
	public List<GroupedTodosResponseDto> findTodos(List<Long> menteeIds, LocalDate startDate, LocalDate endDate, String assignYn, String draftYn) {
		List<Todo> todos;

		if (startDate == null) {
			todos = todoRepository.findByMenteeIdInAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, assignYn, "N", draftYn);
		} else if (endDate == null) {
			todos = todoRepository.findByMenteeIdInAndTodoDateAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, startDate, assignYn, "N", draftYn);
		} else {
			todos = todoRepository.findByMenteeIdInAndTodoDateBetweenAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, startDate, endDate, assignYn, "N", draftYn);
		}

		// N+1 최적화: 필요한 Solution ID 수집
		Set<Long> solutionIds = todos.stream()
				.map(Todo::getSolutionId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Map<Long, String> solutionTitleMap = solutionService.getAllSolutionContent(solutionIds);

		// N+1 최적화: 멘티 이름 수집
		Set<Long> distinctMenteeIds = todos.stream()
				.map(Todo::getMenteeId)
				.collect(Collectors.toSet());
		Map<Long, String> menteeNameMap = memberRepository.findAllById(distinctMenteeIds).stream()
				.collect(Collectors.toMap(Member::getId, Member::getMemName));

		todos.sort(Comparator.comparing(Todo::getTodoDate).reversed());
		Map<LocalDate, List<TodoListResponseDto>> groupedTodos = todos.stream()
				.map(todo -> TodoListResponseDto.builder()
						.todoId(todo.getId())
						.title(todo.getTodoName())
						.content(todo.getTodoNote())
						.subject(todo.getTodoSubjects())
						.solution(solutionTitleMap.get(todo.getSolutionId()))
						.date(todo.getTodoDate())
						.completed("Y".equals(todo.getTodoCompleteYn()))
						.menteeId(todo.getMenteeId())
						.menteeName(menteeNameMap.getOrDefault(todo.getMenteeId(), "알 수 없음"))
						.assignYn(todo.getTodoAssignYn())
						.draftYn(todo.getTodoDraftYn())
						.build())
				.collect(Collectors.groupingBy(TodoListResponseDto::getDate, TreeMap::new, Collectors.toList()));

		return groupedTodos.entrySet().stream()
				.map(entry -> GroupedTodosResponseDto.builder()
						.date(entry.getKey())
						.todos(entry.getValue())
						.build())
				.toList();
	}

	public List<GroupedTodosResponseDto> findTodosWeekly(List<Long> menteeIds, LocalDate mondayDate, String assignYn, String draftYn) {
		LocalDate startDate = mondayDate.with(DayOfWeek.MONDAY);
		LocalDate endDate = mondayDate.plusDays(6);
		return findTodos(menteeIds, startDate, endDate, assignYn, draftYn);
	}

	/**
	 * 할일/과제 상세 조회
	 */
	public TodoDetailResponseDto findTodoDetail(Long todoId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		String solutionTitle = solutionService.getSolutionTitleById(todo.getSolutionId());
		List<FileInfo> solutionWorkbooks = solutionService.parseSolutionFiles(todo.getSolutionId());

		TodoFileData todoFileData = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);

		// 학습지 병합 (솔루션 학습지 + Todo 개별 학습지)
		List<FileInfo> mergedWorkbooks = new ArrayList<>(solutionWorkbooks);
		if (todoFileData != null && todoFileData.getWorkbooks() != null) {
			mergedWorkbooks.addAll(todoFileData.getWorkbooks());
		}

		List<FileInfo> verificationImages = (todoFileData != null && todoFileData.getVerificationImages() != null)
				? todoFileData.getVerificationImages() : Collections.emptyList();

		Feedback feedback = feedbackRepository.findByTodoIdAndDeleteYnAndFeedCompleteYn(todoId, "N", "Y")
				.orElse(null);

		FeedbackDetailResponseDto.FeedbackInfo feedbackInfo = null;
		if (feedback != null) {
			feedbackInfo = FeedbackDetailResponseDto.FeedbackInfo.builder()
					.feedbackId(feedback.getId())
					.content(feedback.getContent())
					.summary(feedback.getSummary())
					.draftYn(feedback.getFeedDraftYn())
					.completeYn(feedback.getFeedCompleteYn())
					.build();
		}

		return TodoDetailResponseDto.builder()
				.todoId(todo.getId())
				.title(todo.getTodoName())
				.content(todo.getTodoNote())
				.solution(solutionTitle)
				.solutionId(todo.getSolutionId())
				.date(todo.getTodoDate())
				.scheduledTime(todo.getScheduledTime())
				.subject(todo.getTodoSubjects())
				.menteeId(todo.getMenteeId())
				.completed("Y".equals(todo.getTodoCompleteYn()))
				.feedbackCompleted(feedback != null && "Y".equals(feedback.getFeedCompleteYn()))
				.assignYn(todo.getTodoAssignYn())
				.draftYn(todo.getTodoDraftYn())
				.workbooks(mergedWorkbooks)
				.studyVerificationImages(verificationImages)
				.feedback(feedbackInfo)
				.build();
	}

	/**
	 * 할일/과제 저장 (다수 날짜 지원)
	 */
	@Transactional
	public void saveTodo(TodoSaveRequestDto request, List<MultipartFile> workbooks) {
		log.info("Saving todo/assignment: title={}, menteeId={}, assignYn={}, draftYn={}",
				request.getTitle(), request.getMenteeId(), request.getAssignYn(), request.getDraftYn());

		List<FileInfo> workbookInfos = new ArrayList<>();
		if (workbooks != null && !workbooks.isEmpty()) {
			workbookInfos = workbooks.stream()
					.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "todos"), null))
					.toList();
		}

		String todoFileJson = workbookInfos.isEmpty() ? null : JsonbUtils.toJson(TodoFileData.withWorkbooks(workbookInfos));

		for (LocalDate date : request.getDates()) {
			Todo todo = Todo.builder()
					.menteeId(request.getMenteeId())
					.todoDate(date)
					.scheduledTime(request.getScheduledTime())
					.todoName(request.getTitle())
					.todoNote(request.getContent())
					.todoSubjects(request.getSubject().getDescription())
					.solutionId(request.getSolutionId())
					.todoFile(todoFileJson)
					.todoAssignYn(Objects.requireNonNullElse(request.getAssignYn(), "N"))
					.todoDraftYn(Objects.requireNonNullElse(request.getDraftYn(), "N"))
					.todoCompleteYn("N")
					.build();

			todoRepository.save(todo);
		}
	}

	/**
	 * 할일/과제 수정
	 */
	@Transactional
	public void updateTodo(Long todoId, TodoSaveRequestDto request, List<MultipartFile> workbooks) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		// 파일 업로드 처리
		String todoFileJson = todo.getTodoFile();
		if (workbooks != null && !workbooks.isEmpty()) {
			List<FileInfo> workbookInfos = workbooks.stream()
					.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "todos"), null))
					.toList();
			
			TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
			TodoFileData updated = (existing != null)
					? existing.updateWorkbooks(workbookInfos)
					: TodoFileData.withWorkbooks(workbookInfos);
			todoFileJson = JsonbUtils.toJson(updated);
		}

		todo.update(
				request.getTitle(),
				request.getContent(),
				request.getSubject().getDescription(),
				request.getDates().get(0), // 단일 수정 시 첫 번째 날짜 사용
				request.getScheduledTime(),
				request.getSolutionId()
		);
		
		if (request.getAssignYn() != null) todo.assign(); // Simple logic to update status if needed
		// Note: Based on Todo entity, we don't have a direct setter for assignYn/draftYn other than markAsDraft/assign
		if ("Y".equals(request.getDraftYn())) todo.markAsDraft();
		
		todo.updateTodoFile(todoFileJson);
	}

	@Transactional
	public void deleteTodo(Long todoId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		todo.delete();
	}

	@Transactional
	public void toggleStatus(Long todoId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		todo.toggleComplete();
	}

	/**
	 * 인증 사진 업로드
	 */
	@Transactional
	public VerificationResponseDto uploadVerification(Long todoId, List<MultipartFile> images) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		List<FileInfo> fileInfos = images.stream()
				.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "todos"), null))
				.toList();

		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImages(fileInfos)
				: TodoFileData.withVerificationImages(fileInfos);

		todo.updateTodoFile(JsonbUtils.toJson(updated));
		todo.markComplete();

		return VerificationResponseDto.builder()
				.imageUrls(fileInfos.stream().map(FileInfo::getFileUrl).toList())
				.build();
	}

	@Transactional
	public VerificationResponseDto updateVerification(Long todoId, List<MultipartFile> images) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if (feedbackRepository.findByTodoIdAndDeleteYnAndFeedCompleteYn(todoId, "N", "Y").isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER, "피드백이 등록되어 수정이 불가합니다.");
		}

		List<FileInfo> fileInfos = images.stream()
				.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "todos"), null))
				.toList();

		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImages(fileInfos)
				: TodoFileData.withVerificationImages(fileInfos);

		todo.updateTodoFile(JsonbUtils.toJson(updated));

		return VerificationResponseDto.builder()
				.imageUrls(fileInfos.stream().map(FileInfo::getFileUrl).toList())
				.build();
	}

	@Transactional
	public VerificationResponseDto deleteVerificationImage(Long todoId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if (feedbackRepository.findByTodoIdAndDeleteYnAndFeedCompleteYn(todoId, "N", "Y").isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER, "피드백이 등록되어 삭제가 불가합니다.");
		}

		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImages(Collections.emptyList())
				: TodoFileData.withVerificationImages(Collections.emptyList());
		
		todo.updateTodoFile(JsonbUtils.toJson(updated));
		todo.markIncomplete();

		return VerificationResponseDto.builder().imageUrls(null).build();
	}

	/**
	 * 멘티의 솔루션 목록 조회 (과제 생성 시 필요)
	 */
	public List<MentorSolutionResponseDto> findSolutionsByTodoId(Long todoId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		List<Solution> solutions = solutionRepository.findByMenteeIdAndDeleteYn(todo.getMenteeId(), "N");

		return solutions.stream()
				.map(solution -> {
					FileInfo file = null;
					String fileJson = solution.getSolutionFile();
					if (fileJson != null && !fileJson.isBlank()) {
						try {
							if (fileJson.trim().startsWith("[")) {
								List<FileInfo> files = JsonbUtils.fromJson(fileJson, new com.fasterxml.jackson.core.type.TypeReference<List<FileInfo>>() {});
								if (files != null && !files.isEmpty()) file = files.get(0);
							} else {
								file = JsonbUtils.fromJson(fileJson, FileInfo.class);
							}
						} catch (Exception ignored) {}
					}
					return new MentorSolutionResponseDto(solution.getId(), solution.getSolutionContent(), 
							(file != null) ? file.getFileName() : null, (file != null) ? file.getFileUrl() : null);
				})
				.toList();
	}

	@Transactional
	public void addTimerSession(Long todoId, TodoTimerSessionCreateRequestDto request) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if (request.getEndAt().isBefore(request.getStartAt())) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER, "종료 시간이 시작 시간보다 빠릅니다.");
		}

		todo.addTimerSession(request.getStartAt(), request.getEndAt());
	}

	public TodoTimerDailyResponseDto getTimersByDate(Long menteeId, LocalDate date) {
		LocalDateTime windowStart = date.atTime(LocalTime.of(5, 0));
		LocalDateTime windowEnd = date.plusDays(1).atTime(LocalTime.of(4, 59));

		List<Todo> todos = todoRepository.findByMenteeIdAndDeleteYn(menteeId, "N");
		ObjectMapper mapper = new ObjectMapper();

		long totalMinutesAll = 0;
		long sessionCountAll = 0;
		List<TodoTimerDailyResponseDto.TodoTimerItem> items = new ArrayList<>();

		for (Todo todo : todos) {
			String timerJson = todo.getTodoTimer();
			if (timerJson == null || timerJson.isBlank()) continue;

			long todoTotalMinutes = 0;
			List<TodoTimerDailyResponseDto.TimerSession> sessionDtos = new ArrayList<>();

			try {
				List<Map<String, String>> sessions = mapper.readValue(timerJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});

				for (Map<String, String> s : sessions) {
					LocalDateTime startAt = LocalDateTime.parse(s.get("startAt"));
					LocalDateTime endAt = LocalDateTime.parse(s.get("endAt"));

					if (endAt.isBefore(startAt) || !isOverlapping(startAt, endAt, windowStart, windowEnd)) continue;

					LocalDateTime clippedStart = max(startAt, windowStart);
					LocalDateTime clippedEnd = min(endAt, windowEnd);

					long minutes = Duration.between(clippedStart, clippedEnd).toMinutes();
					if (minutes <= 0) continue;

					sessionDtos.add(TodoTimerDailyResponseDto.TimerSession.builder()
							.startAt(clippedStart.toString()).endAt(clippedEnd.toString()).minutes(minutes).build());

					todoTotalMinutes += minutes;
					totalMinutesAll += minutes;
					sessionCountAll++;
				}
			} catch (Exception e) {
				log.error("Error parsing timer JSON for todoId={}", todo.getId(), e);
			}
			if (!sessionDtos.isEmpty()) {
				items.add(TodoTimerDailyResponseDto.TodoTimerItem.builder()
						.todoId(todo.getId())
                        .title(todo.getTodoName())
                        .subject(todo.getTodoSubjects())
                        .note(todo.getTodoNote())
                        .name(todo.getTodoName())
                        .assignYn(todo.getTodoAssignYn())
						.totalMinutes(todoTotalMinutes)
                        .sessions(sessionDtos).build());
			}
		}

		return TodoTimerDailyResponseDto.builder()
				.menteeId(menteeId).date(date).totalMinutes(totalMinutesAll)
				.averageMinutes((sessionCountAll == 0) ? 0 : (totalMinutesAll / sessionCountAll))
				.items(items).build();
	}

	private boolean isOverlapping(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
		return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
	}
	private LocalDateTime max(LocalDateTime a, LocalDateTime b) { return a.isAfter(b) ? a : b; }
	private LocalDateTime min(LocalDateTime a, LocalDateTime b) { return a.isBefore(b) ? a : b; }
}
