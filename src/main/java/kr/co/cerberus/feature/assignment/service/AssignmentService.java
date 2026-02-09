package kr.co.cerberus.feature.assignment.service;

import kr.co.cerberus.feature.assignment.dto.AssignmentDetailResponseDto;
import kr.co.cerberus.feature.assignment.dto.AssignmentListResponseDto;
import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.dto.FeedbackDetailResponseDto;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.report.service.WeeklyReportService;
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

import kr.co.cerberus.feature.assignment.dto.GroupedAssignmentsResponseDto;

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
	private final SolutionService solutionService;
	private final FileStorageService fileStorageService;
	private final WeeklyReportService weeklyReportService;
	
	public List<GroupedAssignmentsResponseDto> findAssignments(Long menteeId, LocalDate startDate, LocalDate endDate) {
		List<Todo> assignments;

		if (startDate == null) {
			assignments = todoRepository.findByMenteeIdAndTodoAssignYnAndDeleteYn(menteeId, "Y", "N");
		} else if (endDate == null) {
			assignments = todoRepository.findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYn(menteeId, startDate, "Y", "N");
		} else {
			assignments = todoRepository.findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYn(menteeId, startDate, endDate, "Y", "N");
		}

		Set<Long> solutionIds = assignments.stream()
				.map(Todo::getSolutionId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> solutionTitleMap = solutionService.getAllSolutionContent(solutionIds);

        assignments.sort(Comparator.comparing(Todo::getTodoDate).reversed());
		Map<LocalDate, List<AssignmentListResponseDto>> groupedAssignments = assignments.stream()
				.map(todo -> AssignmentListResponseDto.builder()
							.assignmentId(todo.getId())
							.title(todo.getTodoName())
							.subject(todo.getTodoSubjects())
							.solution(solutionTitleMap.get(todo.getSolutionId()))
							.date(todo.getTodoDate())
							.completed("Y".equals(todo.getTodoCompleteYn()))
							.build())
				.collect(Collectors.groupingBy(AssignmentListResponseDto::getDate, TreeMap::new, Collectors.toList()));

		return groupedAssignments.entrySet().stream()
				.map(entry -> GroupedAssignmentsResponseDto.builder()
						.date(entry.getKey())
						.assignments(entry.getValue())
						.build())
				.toList();
	}

	public List<GroupedAssignmentsResponseDto> findAssignmentsWeekly(Long menteeId, LocalDate mondayDate) {
		LocalDate startDate = mondayDate.with(DayOfWeek.MONDAY);
		LocalDate endDate = mondayDate.plusDays(6);
		return findAssignments(menteeId, startDate, endDate);
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

		Feedback feedback = feedbackRepository.findByTodoIdAndDeleteYn(assignmentId, "N")
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
				.date(todo.getTodoDate())
				.completed("Y".equals(todo.getTodoCompleteYn()))
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