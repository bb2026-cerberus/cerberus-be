package kr.co.cerberus.feature.assignment.service;

import kr.co.cerberus.feature.assignment.dto.AssignmentDetailResponseDto;
import kr.co.cerberus.feature.assignment.dto.AssignmentListResponseDto;
import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.solution.service.SolutionService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kr.co.cerberus.feature.assignment.dto.GroupedAssignmentsResponseDto;

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

	public List<GroupedAssignmentsResponseDto> findAssignments(Long menteeId, LocalDate startDate, LocalDate endDate) {
		List<Todo> assignments;

		if (startDate == null) {
			assignments = todoRepository.findByMenteeIdAndTodoAssignYnAndDeleteYn(menteeId, "Y", "N");
		} else if (endDate == null) {
			assignments = todoRepository.findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYn(menteeId, startDate, "Y", "N");
		} else {
			assignments = todoRepository.findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYn(menteeId, startDate, endDate, "Y", "N");
		}

		// N+1 최적화: 필요한 Solution ID들을 수집하여 한 번에 조회
		Set<Long> solutionIds = assignments.stream()
				.map(Todo::getSolutionId)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		Map<Long, String> solutionTitleMap = solutionService.getAllSolutionTitle(solutionIds);

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
		LocalDate startDate = mondayDate;
		LocalDate endDate = mondayDate.plusDays(6);
		return findAssignments(menteeId, startDate, endDate);
	}

	public AssignmentDetailResponseDto findAssignmentDetail(Long assignmentId) {
		Todo todo = findAssignmentById(assignmentId);

		// TODO: 보안 - 요청한 사용자가 해당 과제의 소유자인지(menteeId 일치 여부) 확인 로직 추가
		String solutionTitle = solutionService.getSolutionTitleById(todo.getSolutionId());

		// solution의 solutionFile JSONB 파싱 (Solution에 연결된 학습지)
		List<FileInfo> solutionWorkbooks = solutionService.parseSolutionFiles(todo.getSolutionId());

		// todoFile JSONB 파싱
		TodoFileData todoFileData = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);

		// todoFileData의 workbooks와 solutionWorkbooks 병합
		List<FileInfo> mergedWorkbooks = new java.util.ArrayList<>(solutionWorkbooks);
		if (todoFileData != null && todoFileData.getWorkbooks() != null) {
			List<FileInfo> todoFileWorkbooks = todoFileData.getWorkbooks();
			mergedWorkbooks.addAll(todoFileWorkbooks);
		}

		// 피드백 JSONB 파싱
		String feedbackContent = feedbackRepository.findByTodoIdAndDeleteYn(assignmentId, "N")
				.map(feedback -> {
					FeedbackFileData data = JsonbUtils.fromJson(feedback.getFeedFile(), FeedbackFileData.class);
					return data != null ? data.getContent() : null;
				})
				.orElse(null);

		// todoFileData의 verificationImages JSONB 파싱
		List<FileInfo> verificationImages = Collections.emptyList();
		if (todoFileData != null && todoFileData.getVerificationImages() != null) {
			verificationImages = todoFileData.getVerificationImages();
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
				.feedback(feedbackContent)
				.build();
	}

	@Transactional
	public VerificationResponseDto uploadVerification(Long assignmentId, List<MultipartFile> images) {
		Todo todo = findAssignmentById(assignmentId);

		// 모든 파일을 저장하고 FileInfo 리스트 생성
		List<FileInfo> fileInfos = images.stream()
				.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "assignments"), null))
				.toList();

		// todoFile JSONB에 전체 파일 리스트 저장
		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImages(fileInfos)
				: TodoFileData.withVerificationImages(fileInfos);

		todo.updateTodoFile(JsonbUtils.toJson(updated));

		// 인증 사진 업로드 시 자동으로 완료 표시 전환
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
		Todo todo = findAssignmentById(assignmentId);

		// 피드백 존재 시 인증사진 수정 불가
		Optional<Feedback> feedback = feedbackRepository.findByTodoIdAndDeleteYn(assignmentId, "N");
		if (feedback.isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER);
		}

		// 모든 새 파일 저장
		List<FileInfo> fileInfos = images.stream()
				.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "assignments"), null))
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
	public VerificationResponseDto deleteVerificationImage(Long assignmentId) {
		Todo todo = findAssignmentById(assignmentId);

		// 피드백 존재 시 인증사진 삭제 불가
		Optional<Feedback> feedback = feedbackRepository.findByTodoIdAndDeleteYn(assignmentId, "N");
		if (feedback.isPresent()) {
			throw new CustomException(ErrorCode.INVALID_PARAMETER);
		}

		// todoFile JSONB 파싱 -> 인증 사진 URL을 null로 설정
		TodoFileData existing = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
		TodoFileData updated = (existing != null)
				? existing.updateVerificationImages(Collections.emptyList())
				: TodoFileData.withVerificationImages(Collections.emptyList());
		todo.updateTodoFile(JsonbUtils.toJson(updated));

		// 인증 사진 삭제 시 미완료 상태로 전환
		todo.markIncomplete();

		return VerificationResponseDto.builder()
				.imageUrls(null)
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
}
