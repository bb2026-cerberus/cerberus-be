package kr.co.cerberus.feature.assignment.service;

import kr.co.cerberus.feature.assignment.dto.*;
import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.dto.FeedbackDetailResponseDto;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.solution.Solution;
import kr.co.cerberus.feature.solution.repository.SolutionRepository;
import kr.co.cerberus.feature.solution.service.SolutionService;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.dto.VerificationResponseDto;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.jsonb.FileInfo;
import kr.co.cerberus.global.jsonb.TodoFileData;
import kr.co.cerberus.global.util.FileStorageService;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {

	private final TodoRepository todoRepository;
	private final FeedbackRepository feedbackRepository;
	private final SolutionRepository solutionRepository;
	private final SolutionService solutionService;
	private final FileStorageService fileStorageService;
	private final MemberRepository memberRepository;

	public List<GroupedAssignmentsResponseDto> findAssignments(List<Long> menteeIds, LocalDate startDate, LocalDate endDate) {
		List<Todo> assignments;

		if (startDate == null) {
			assignments = todoRepository.findByMenteeIdInAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, "Y", "N", "N");
		} else if (endDate == null) {
			assignments = todoRepository.findByMenteeIdInAndTodoDateAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, startDate, "Y", "N", "N");
		} else {
			assignments = todoRepository.findByMenteeIdInAndTodoDateBetweenAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, startDate, endDate, "Y", "N", "N");
		}

		Set<Long> solutionIds = assignments.stream()
				.map(Todo::getSolutionId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> solutionTitleMap = solutionService.getAllSolutionContent(solutionIds);

		// 멘티 이름 수집
		Set<Long> distinctMenteeIds = assignments.stream()
				.map(Todo::getMenteeId)
				.collect(Collectors.toSet());
		Map<Long, String> menteeNameMap = memberRepository.findAllById(distinctMenteeIds).stream()
				.collect(Collectors.toMap(Member::getId, Member::getMemName));

        assignments.sort(Comparator.comparing(Todo::getTodoDate).reversed());
		Map<LocalDate, List<AssignmentListResponseDto>> groupedAssignments = assignments.stream()
				.map(todo -> AssignmentListResponseDto.builder()
							.assignmentId(todo.getId())
							.title(todo.getTodoName())
							.subject(todo.getTodoSubjects())
							.solution(solutionTitleMap.get(todo.getSolutionId()))
							.date(todo.getTodoDate())
							.completed("Y".equals(todo.getTodoCompleteYn()))
							.menteeId(todo.getMenteeId())
							.menteeName(menteeNameMap.getOrDefault(todo.getMenteeId(), "알 수 없음"))
							.build())
				.collect(Collectors.groupingBy(AssignmentListResponseDto::getDate, TreeMap::new, Collectors.toList()));

		return groupedAssignments.entrySet().stream()
				.map(entry -> GroupedAssignmentsResponseDto.builder()
						.date(entry.getKey())
						.assignments(entry.getValue())
						.build())
				.toList();
	}

	public List<GroupedAssignmentsResponseDto> findAssignmentsWeekly(List<Long> menteeIds, LocalDate mondayDate) {
		LocalDate startDate = mondayDate.with(DayOfWeek.MONDAY);
		LocalDate endDate = mondayDate.plusDays(6);
		return findAssignments(menteeIds, startDate, endDate);
	}

	public List<GroupedAssignmentsResponseDto> findDraftAssignments(List<Long> menteeIds) {
		List<Todo> assignments = todoRepository.findByMenteeIdInAndTodoAssignYnAndDeleteYnAndTodoDraftYn(menteeIds, "Y", "N", "Y");

		Set<Long> solutionIds = assignments.stream()
				.map(Todo::getSolutionId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> solutionTitleMap = solutionService.getAllSolutionContent(solutionIds);

		// 멘티 이름 수집
		Set<Long> distinctMenteeIds = assignments.stream()
				.map(Todo::getMenteeId)
				.collect(Collectors.toSet());
		Map<Long, String> menteeNameMap = memberRepository.findAllById(distinctMenteeIds).stream()
				.collect(Collectors.toMap(Member::getId, Member::getMemName));

		assignments.sort(Comparator.comparing(Todo::getTodoDate).reversed());
		Map<LocalDate, List<AssignmentListResponseDto>> groupedAssignments = assignments.stream()
				.map(todo -> AssignmentListResponseDto.builder()
						.assignmentId(todo.getId())
						.title(todo.getTodoName())
						.subject(todo.getTodoSubjects())
						.solution(solutionTitleMap.get(todo.getSolutionId()))
						.date(todo.getTodoDate())
						.completed("Y".equals(todo.getTodoCompleteYn()))
						.menteeId(todo.getMenteeId())
						.menteeName(menteeNameMap.getOrDefault(todo.getMenteeId(), "알 수 없음"))
						.build())
				.collect(Collectors.groupingBy(AssignmentListResponseDto::getDate, TreeMap::new, Collectors.toList()));

		return groupedAssignments.entrySet().stream()
				.map(entry -> GroupedAssignmentsResponseDto.builder()
						.date(entry.getKey())
						.assignments(entry.getValue())
						.build())
				.toList();
	}

	/**
	 * todoId를 통해 menteeId를 찾고, 해당 멘티의 모든 솔루션 목록을 반환합니다.
	 */
	public List<MentorSolutionResponseDto> findSolutionsByTodoId(Long todoId) {
		Todo todo = todoRepository.findById(todoId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		List<Solution> solutions = solutionRepository.findByMenteeId(todo.getMenteeId());

		return solutions.stream()
				.map(solution -> {
					String fileJson = solution.getSolutionFile();
					FileInfo file = null;
					
					if (fileJson != null && !fileJson.isBlank()) {
						try {
							if (fileJson.trim().startsWith("[")) {
								// 리스트 형태인 경우 (하위 호환성)
								List<FileInfo> files = JsonbUtils.fromJson(fileJson, new com.fasterxml.jackson.core.type.TypeReference<List<FileInfo>>() {});
								if (files != null && !files.isEmpty()) {
									file = files.get(0);
								}
							} else {
								// 단일 객체 형태
								file = JsonbUtils.fromJson(fileJson, FileInfo.class);
							}
						} catch (Exception e) {
							// ignore
						}
					}

					String fileName = (file != null) ? file.getFileName() : null;
					String fileUrl = (file != null) ? file.getFileUrl() : null;
					return new MentorSolutionResponseDto(solution.getId(), solution.getSolutionContent(), fileName, fileUrl);
				})
				.toList();
	}

	/**
	 * 멘토가 과제를 생성합니다. (다수 날짜 처리 및 학습지 업로드 포함)
	 */
	@Transactional
	public void createAssignmentByMentor(MentorAssignmentCreateRequestDto request, List<MultipartFile> workbooks) {
		List<FileInfo> workbookInfos = new ArrayList<>();
		if (workbooks != null && !workbooks.isEmpty()) {
			workbookInfos = workbooks.stream()
					.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "assignments"), null))
					.toList();
		}

		String todoFileJson = JsonbUtils.toJson(TodoFileData.withWorkbooks(workbookInfos));
		String draftYn = Boolean.TRUE.equals(request.isDraft()) ? "Y" : "N";

		for (LocalDate date : request.dates()) {
			Todo assignment = Todo.builder()
					.menteeId(request.menteeId())
					.todoDate(date)
					.todoName(request.title())
					.todoNote(request.content())
					.todoSubjects(request.subject().getDescription())
					.solutionId(request.solutionId())
					.todoFile(todoFileJson)
					.todoAssignYn("Y")
					.todoCompleteYn("N")
					.todoDraftYn(draftYn)
					.build();

			todoRepository.save(assignment);
		}
	}

	@Transactional
	public void updateAssignment(Long assignmentId, MentorAssignmentCreateRequestDto request, List<MultipartFile> workbooks) {
		Todo assignment = todoRepository.findById(assignmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if (!"Y".equals(assignment.getTodoAssignYn())) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER, "멘토 과제만 수정 가능합니다.");
		}

		// 파일 업로드 처리 (새로운 파일이 있을 때만 업데이트)
		String todoFileJson = assignment.getTodoFile();
		if (workbooks != null && !workbooks.isEmpty()) {
			List<FileInfo> workbookInfos = workbooks.stream()
					.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "assignments"), null))
					.toList();
			
			TodoFileData existing = JsonbUtils.fromJson(assignment.getTodoFile(), TodoFileData.class);
			TodoFileData updated = (existing != null)
					? existing.updateWorkbooks(workbookInfos)
					: TodoFileData.withWorkbooks(workbookInfos);
			todoFileJson = JsonbUtils.toJson(updated);
		}

		assignment.update(
				request.title(),
				request.content(),
				request.subject().getDescription(),
				request.dates().get(0), // 단일 수정 시 첫 번째 날짜 사용
				request.solutionId()
		);
		assignment.updateTodoFile(todoFileJson);
	}

	@Transactional
	public void deleteAssignment(Long assignmentId) {
		Todo assignment = todoRepository.findById(assignmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		assignment.delete();
	}

	public AssignmentDetailResponseDto findAssignmentDetail(Long assignmentId) {
		Todo todo = todoRepository.findById(assignmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		if (!"Y".equals(todo.getTodoAssignYn())) {
			throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
		}

		String solutionTitle = solutionService.getSolutionTitleById(todo.getSolutionId());
		List<FileInfo> solutionWorkbooks = solutionService.parseSolutionFiles(todo.getSolutionId());
		TodoFileData todoFileData = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);

		List<FileInfo> mergedWorkbooks = new java.util.ArrayList<>(solutionWorkbooks);
		if (todoFileData != null && todoFileData.getWorkbooks() != null) {
			mergedWorkbooks.addAll(todoFileData.getWorkbooks());
		}

		List<FileInfo> verificationImages = Collections.emptyList();
		if (todoFileData != null && todoFileData.getVerificationImages() != null) {
			verificationImages = todoFileData.getVerificationImages();
		}

		Feedback feedback = feedbackRepository.findByTodoIdAndDeleteYnAndFeedCompleteYn(assignmentId, "N", "Y")
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

		return AssignmentDetailResponseDto.builder()
				.assignmentId(todo.getId())
				.title(todo.getTodoName())
				.content(todo.getTodoNote())
				.solution(solutionTitle)
				.solutionId(todo.getSolutionId())
				.menteeId(todo.getMenteeId())
				.date(todo.getTodoDate())
				.assignmentCompleted("Y".equals(todo.getTodoCompleteYn()))
				.feedbackCompleted("Y".equals(feedback != null && "Y".equals(feedback.getFeedCompleteYn()) ? "Y" : "N"))
				.subject(todo.getTodoSubjects())
				.workbooks(mergedWorkbooks)
				.studyVerificationImages(verificationImages)
				.feedback(feedbackInfo)
				.build();
	}

	@Transactional
	public VerificationResponseDto uploadVerification(Long assignmentId, List<MultipartFile> images) {
		Todo todo = todoRepository.findById(assignmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		List<FileInfo> fileInfos = images.stream()
				.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "assignments"), null))
				.toList();

		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImages(fileInfos)
				: TodoFileData.withVerificationImages(fileInfos);

		todo.updateTodoFile(JsonbUtils.toJson(updated));
		todo.markComplete();

		List<String> imageUrls = fileInfos.stream()
				.map(FileInfo::getFileUrl)
				.toList();

		return VerificationResponseDto.builder()
				.imageUrls(imageUrls)
				.build();
	}

	@Transactional
	public VerificationResponseDto updateVerification(Long assignmentId, List<MultipartFile> images) {
		Todo todo = todoRepository.findById(assignmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		// 피드백 존재 시 인증사진 수정 불가
		Optional<Feedback> feedback = feedbackRepository.findByTodoIdAndDeleteYnAndFeedCompleteYn(assignmentId, "N", "Y");
		if (feedback.isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER);
		}

		// 모든 새 파일 저장
		List<FileInfo> fileInfos = images.stream()
				.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "assignments"), null))
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
	public VerificationResponseDto deleteVerificationImage(Long assignmentId) {
		Todo todo = todoRepository.findById(assignmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

		// 피드백 존재 시 인증사진 삭제 불가
		Optional<Feedback> feedback = feedbackRepository.findByTodoIdAndDeleteYnAndFeedCompleteYn(assignmentId, "N", "Y");
		if (feedback.isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER);
		}

		// todoFile JSONB 파싱 -> 인증 사진 URL을 null로 설정
		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImages(Collections.emptyList())
				: TodoFileData.withVerificationImages(Collections.emptyList());
		todo.updateTodoFile(JsonbUtils.toJson(updated));

		todo.markIncomplete();

		return VerificationResponseDto.builder()
				.imageUrls(null)
				.build();
	}
}