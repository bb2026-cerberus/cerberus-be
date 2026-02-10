package kr.co.cerberus.feature.todo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.todo.dto.MentorSolutionResponseDto;
import kr.co.cerberus.feature.feedback.service.FeedbackService;
import kr.co.cerberus.feature.todo.dto.*;
import kr.co.cerberus.feature.todo.service.TodoService;
import kr.co.cerberus.global.common.CommonResponse;
import kr.co.cerberus.global.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Todo", description = "할일 및 과제 통합 API")
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

	private final TodoService todoService;
	private final FileStorageService fileStorageService;
	private final FeedbackService feedbackService;

	@Operation(summary = "할일/과제 목록 조회", description = "파라미터에 따라 할일 또는 과제 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<CommonResponse<List<GroupedTodosResponseDto>>> getTodos(
			@Parameter(description = "멘토 ID", example = "2") @RequestParam(value = "mentorId", required = false) Long mentorId,
			@Parameter(description = "멘티 ID 목록", example = "2,3") @RequestParam(value = "menteeId", required = false) List<Long> menteeIds,
			@Parameter(description = "시작 날짜 (YYYY-MM-DD)") @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@Parameter(description = "종료 날짜 (YYYY-MM-DD)") @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@Parameter(description = "과제 여부 (Y/N)", example = "N") @RequestParam(value = "assignYn", defaultValue = "N") String assignYn,
			@Parameter(description = "임시저장 여부 (Y/N)", example = "N") @RequestParam(value = "draftYn", defaultValue = "N") String draftYn
	) {
		List<GroupedTodosResponseDto> todos = todoService.findTodos(mentorId, menteeIds, startDate, endDate, assignYn, draftYn);
		return ResponseEntity.ok(CommonResponse.of(todos));
	}

	@Operation(summary = "주차별 목록 조회", description = "특정 주차의 월요일 날짜를 기준으로 할일/과제 목록을 조회")
	@GetMapping("/weekly")
	public ResponseEntity<CommonResponse<List<GroupedTodosResponseDto>>> getTodosWeekly(
			@Parameter(description = "멘토 ID", example = "2") @RequestParam(value = "mentorId") Long mentorId,
			@Parameter(description = "멘티 ID 목록", example = "2,3") @RequestParam(value = "menteeId") List<Long> menteeIds,
			@Parameter(description = "월요일 날짜 (YYYY-MM-DD)") @RequestParam(value = "mondayDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mondayDate,
			@Parameter(description = "과제 여부 (Y/N)", example = "N") @RequestParam(value = "assignYn", defaultValue = "N") String assignYn,
			@Parameter(description = "임시저장 여부 (Y/N)", example = "N") @RequestParam(value = "draftYn", defaultValue = "N") String draftYn
	) {
		List<GroupedTodosResponseDto> weeklyTodos = todoService.findTodosWeekly(mentorId, menteeIds, mondayDate, assignYn, draftYn);
		return ResponseEntity.ok(CommonResponse.of(weeklyTodos));
	}

	@Operation(summary = "상세 조회", description = "할일/과제 ID를 기반으로 상세 정보를 조회합니다.")
	@GetMapping("/{todoId}")
	public ResponseEntity<CommonResponse<TodoDetailResponseDto>> getTodoDetail(@PathVariable Long todoId) {
		return ResponseEntity.ok(CommonResponse.of(todoService.findTodoDetail(todoId)));
	}

	@Operation(summary = "등록 및 임시저장", description = "새로운 할일 또는 과제를 등록합니다. 과제인 경우 학습지 파일을 첨부할 수 있습니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<Void>> createTodo(
			@RequestPart("request") TodoSaveRequestDto request,
			@RequestPart(value = "workbooks", required = false) List<MultipartFile> workbooks
	) {
		todoService.saveTodo(request, workbooks);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "수정", description = "기존 정보를 수정합니다.")
	@PutMapping(value = "/{todoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<Void>> updateTodo(
			@PathVariable Long todoId,
			@RequestPart("request") TodoSaveRequestDto request,
			@RequestPart(value = "workbooks", required = false) List<MultipartFile> workbooks
	) {
		todoService.updateTodo(todoId, request, workbooks);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "삭제", description = "할일/과제를 삭제합니다.")
	@DeleteMapping("/{todoId}")
	public ResponseEntity<CommonResponse<Void>> deleteTodo(@PathVariable Long todoId) {
		todoService.deleteTodo(todoId);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "완료 상태 토글", description = "완료 상태를 토글합니다.")
	@PatchMapping("/{todoId}/completed")
	public ResponseEntity<CommonResponse<Void>> toggleStatus(@PathVariable Long todoId) {
		todoService.toggleStatus(todoId);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "공부 인증 사진 업로드", description = "인증 사진을 업로드합니다. 업로드 시 자동으로 완료 상태로 전환")
	@PostMapping(value = "/{todoId}/verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<VerificationResponseDto>> uploadVerification(
			@PathVariable Long todoId,
			@RequestPart("images") List<MultipartFile> images
	) {
		VerificationResponseDto response = todoService.uploadVerification(todoId, images);
		feedbackService.analyzeTodoImagesAsync(todoId);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "공부 인증 사진 수정", description = "기존 인증 사진을 새 사진으로 수정")
	@PutMapping(value = "/{todoId}/verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<VerificationResponseDto>> updateVerification(
			@PathVariable Long todoId,
			@RequestPart("images") List<MultipartFile> images
	) {
		VerificationResponseDto response = todoService.updateVerification(todoId, images);
		feedbackService.analyzeTodoImagesAsync(todoId);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "공부 인증 사진 삭제", description = "인증 사진을 삭제하고 상태를 미완료로 변경")
	@DeleteMapping("/{todoId}/verification")
	public ResponseEntity<CommonResponse<VerificationResponseDto>> deleteVerification(@PathVariable Long todoId) {
		return ResponseEntity.ok(CommonResponse.of(todoService.deleteVerificationImage(todoId)));
	}

	@Operation(summary = "학습지/파일 다운로드", description = "파일 URL을 통해 파일을 다운로드")
	@GetMapping("/download")
	public ResponseEntity<Resource> downloadFile(@RequestParam("fileUrl") String fileUrl) {
		Resource resource = fileStorageService.loadFileAsResource(fileUrl);
		String contentType = fileStorageService.getContentType(resource);
		String filename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(resource);
	}

	@Operation(summary = "멘티의 솔루션 목록 조회", description = "과제 생성 시 멘티의 보완점 목록을 조회합니다.")
	@GetMapping("/solutions")
	public ResponseEntity<CommonResponse<List<MentorSolutionResponseDto>>> getMenteeSolutions(@RequestParam Long todoId) {
		return ResponseEntity.ok(CommonResponse.of(todoService.findSolutionsByTodoId(todoId)));
	}

	@Operation(summary = "타이머 세션 추가", description = "타이머 세션을 추가합니다.")
	@PostMapping("/{todoId}/timer/sessions")
	public ResponseEntity<CommonResponse<Void>> addTimerSession(
			@PathVariable Long todoId,
			@RequestBody TodoTimerSessionCreateRequestDto request
	) {
		todoService.addTimerSession(todoId, request);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "일별 타이머 조회", description = "특정 날짜의 타이머 기록을 조회합니다.")
	@GetMapping("/timers/daily")
	public ResponseEntity<CommonResponse<TodoTimerDailyResponseDto>> getTimersByDate(
			@RequestParam("menteeId") Long menteeId,
			@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return ResponseEntity.ok(CommonResponse.of(todoService.getTimersByDate(menteeId, date)));
	}

    @Operation(summary = "홈 일별 조회", description = "특정 날짜의 할일/과제 개요를 조회합니다.")
    @GetMapping("/daily/overview")
    public ResponseEntity<CommonResponse<TodoDailyOverviewResponseDto>> getDailyOverview(
            @RequestParam("menteeId") Long menteeId,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date == null) ? LocalDate.now() : date;

        return ResponseEntity.ok(
                CommonResponse.of(todoService.getDailyOverview(menteeId, targetDate))
        );
    }

}