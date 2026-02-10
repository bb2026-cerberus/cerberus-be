package kr.co.cerberus.feature.todo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.dto.FeedbackDetailResponseDto;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.repository.MemberRepository;
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

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
	private final MemberRepository memberRepository;

	public List<GroupedTodosResponseDto> findTodos(List<Long> menteeIds, LocalDate startDate, LocalDate endDate) {
		List<Todo> todos;

		if (startDate == null) {
			todos = todoRepository.findByMenteeIdInAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, "N", "N", "N");
		} else if (endDate == null) {
			todos = todoRepository.findByMenteeIdInAndTodoDateAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, startDate, "N", "N", "N");
		} else {
			todos = todoRepository.findByMenteeIdInAndTodoDateBetweenAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, startDate, endDate, "N", "N", "N");
		}

		// N+1 최적화: 필요한 Solution ID들을 수집하여 한 번에 조회
		Set<Long> solutionIds = todos.stream()
				.map(Todo::getSolutionId)
				.filter(java.util.Objects::nonNull)
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
						.subject(todo.getTodoSubjects())
						.solution(solutionTitleMap.get(todo.getSolutionId()))
						.date(todo.getTodoDate())
						.completed("Y".equals(todo.getTodoCompleteYn()))
						.menteeId(todo.getMenteeId())
						.menteeName(menteeNameMap.getOrDefault(todo.getMenteeId(), "알 수 없음"))
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

		Set<Long> solutionIds = todos.stream()
				.map(Todo::getSolutionId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> solutionTitleMap = solutionService.getAllSolutionContent(solutionIds);

		// 멘티 이름 (단일 멘티)
		String menteeName = memberRepository.findById(menteeId)
				.map(Member::getMemName)
				.orElse("알 수 없음");

		todos.sort(Comparator.comparing(Todo::getTodoDate).reversed());
		Map<LocalDate, List<TodoListResponseDto>> groupedTodos = todos.stream()
				.map(todo -> TodoListResponseDto.builder()
						.todoId(todo.getId())
						.title(todo.getTodoName())
						.subject(todo.getTodoSubjects())
						.solution(solutionTitleMap.get(todo.getSolutionId()))
						.date(todo.getTodoDate())
						.completed("Y".equals(todo.getTodoCompleteYn()))
						.menteeId(todo.getMenteeId())
						.menteeName(menteeName)
						.build())
				.collect(Collectors.groupingBy(TodoListResponseDto::getDate, TreeMap::new, Collectors.toList()));

		return groupedTodos.entrySet().stream()
				.map(entry -> GroupedTodosResponseDto.builder()
						.date(entry.getKey())
						.todos(entry.getValue())
						.build())
				.toList();
	}

	public List<GroupedTodosResponseDto> findTodosWeekly(List<Long> menteeIds, LocalDate mondayDate) {
		LocalDate startDate = mondayDate.with(DayOfWeek.MONDAY);
		LocalDate endDate = mondayDate.plusDays(6);
		return findTodos(menteeIds, startDate, endDate);
	}

	public TodoDetailResponseDto findTodoDetail(Long todoId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		
		String solutionTitle = solutionService.getSolutionTitleById(todo.getSolutionId());
		List<FileInfo> solutionWorkbooks = solutionService.parseSolutionFiles(todo.getSolutionId());

		TodoFileData todoFileData = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);

		List<FileInfo> verificationImages = Collections.emptyList();
		if (todoFileData != null && todoFileData.getVerificationImages() != null) {
			verificationImages = todoFileData.getVerificationImages();
		}

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
				.todoCompleted("Y".equals(todo.getTodoCompleteYn()))
				.feedbackCompleted("Y".equals(feedback != null && "Y".equals(feedback.getFeedCompleteYn()) ? "Y" : "N"))
				.subject(todo.getTodoSubjects())
				.workbooks(solutionWorkbooks)
				.studyVerificationImages(verificationImages)
				.feedback(feedbackInfo)
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
	public void toggleStatus(Long todoId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		todo.toggleComplete();
	}
	
	public void markComplete(Long todoId) {
		Todo todo = findById(todoId);
		todo.markComplete();
	}

	@Transactional
	public VerificationResponseDto uploadTodoFile(Long todoId, List<MultipartFile> files) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		List<FileInfo> fileInfos = files.stream()
				.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "todos"), null))
				.toList();

		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateWorkbooks(fileInfos)
				: TodoFileData.withWorkbooks(fileInfos);

		todo.updateTodoFile(JsonbUtils.toJson(updated));

		List<String> imageUrls = fileInfos.stream()
				.map(FileInfo::getFileUrl)
				.toList();

		return VerificationResponseDto.builder()
				.imageUrls(imageUrls)
				.build();
	}

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
		Optional<Feedback> feedback = feedbackRepository.findByTodoIdAndDeleteYnAndFeedCompleteYn(todoId, "N", "Y");
		if (feedback.isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER, "피드백이 등록되어 수정이 불가합니다.");
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
		Optional<Feedback> feedback = feedbackRepository.findByTodoIdAndDeleteYnAndFeedCompleteYn(todoId, "N", "Y");
		if (feedback.isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER, "피드백이 등록되어 삭제가 불가합니다.");
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
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if (request.getEndAt().isBefore(request.getStartAt())) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER, "종료 시간이 시작 시간보다 빠릅니다.");
		}

		todo.addTimerSession(request.getStartAt(), request.getEndAt());
	}

	@Transactional
	public void deleteTodo(Long todoId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		todo.delete();
	}

	@Transactional
	public TodoCreateResponseDto updateTodo(Long todoId, TodoCreateRequestDto request) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		todo.update(
				request.getTitle(),
				request.getContent(),
				request.getSubject().getDescription(),
				request.getDate(),
				request.getSolutionId()
		);

		return TodoCreateResponseDto.builder()
				.todoId(todo.getId())
				.title(todo.getTodoName())
				.content(todo.getTodoNote())
				.subject(todo.getTodoSubjects())
				.solution(solutionService.getSolutionTitleById(todo.getSolutionId()))
				.date(todo.getTodoDate())
				.completed("Y".equals(todo.getTodoCompleteYn()))
				.build();
	}

	@Transactional
	public void deleteDraftTodo(Long todoId) {
		Todo todo = findById(todoId);

		// 임시저장 상태인지 확인
		if (!"Y".equals(todo.getTodoDraftYn())) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER);
		}

		todo.delete();
	}

    public TodoTimerDailyResponseDto getTimersByDate(Long menteeId, LocalDate date) {

        LocalDateTime windowStart = date.atTime(LocalTime.of(5, 0));              // 05:00
        LocalDateTime windowEnd = date.plusDays(1).atTime(LocalTime.of(4, 59));    // 다음날 04:00

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
                // todo_timer: [{"startAt":"...","endAt":"..."}]
                List<Map<String, String>> sessions = mapper.readValue(
                        timerJson, new TypeReference<List<Map<String, String>>>() {}
                );

                for (Map<String, String> s : sessions) {
                    String startStr = s.get("startAt");
                    String endStr = s.get("endAt");
                    if (startStr == null || endStr == null) continue;

                    LocalDateTime startAt = LocalDateTime.parse(startStr);
                    LocalDateTime endAt = LocalDateTime.parse(endStr);
                    if (endAt.isBefore(startAt)) continue;

                    // ✅ 시간창과 겹치는 세션만 포함 (겹치면 clip해서 계산)
                    if (!isOverlapping(startAt, endAt, windowStart, windowEnd)) continue;

                    LocalDateTime clippedStart = max(startAt, windowStart);
                    LocalDateTime clippedEnd = min(endAt, windowEnd);

                    long minutes = Duration.between(clippedStart, clippedEnd).toMinutes();
                    if (minutes <= 0) continue;

                    sessionDtos.add(TodoTimerDailyResponseDto.TimerSession.builder()
                            .startAt(clippedStart.toString())
                            .endAt(clippedEnd.toString())
                            .minutes(minutes)
                            .build());

                    todoTotalMinutes += minutes;
                    totalMinutesAll += minutes;
                    sessionCountAll++;
                }

            } catch (Exception e) {
                throw new RuntimeException("Todo timer JSON 파싱 오류 (todoId=" + todo.getId() + ")", e);
            }

            if (!sessionDtos.isEmpty()) {
                items.add(TodoTimerDailyResponseDto.TodoTimerItem.builder()
                        .todoId(todo.getId())
                        .title(todo.getTodoName())
                        .subject(todo.getTodoSubjects())
                        .totalMinutes(todoTotalMinutes)
                        .sessions(sessionDtos)
                        .build());
            }
        }

        long averageMinutes = (sessionCountAll == 0) ? 0 : (totalMinutesAll / sessionCountAll);

        return TodoTimerDailyResponseDto.builder()
                .menteeId(menteeId)
                .date(date)
                .totalMinutes(totalMinutesAll)
                .averageMinutes(averageMinutes)
                .items(items)
                .build();
    }

    // ===== helper =====
    private boolean isOverlapping(LocalDateTime aStart, LocalDateTime aEnd,
                                  LocalDateTime bStart, LocalDateTime bEnd) {
        // [aStart, aEnd) 와 [bStart, bEnd) 가 겹치는지
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}