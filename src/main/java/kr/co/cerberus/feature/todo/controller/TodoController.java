package kr.co.cerberus.feature.todo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Todo", description = "할일 관련 API")
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

	private final TodoService todoService;
	private final FileStorageService fileStorageService;
	private final FeedbackService feedbackService;

	@Operation(summary = "할일 목록 조회", description = "전체/기간별/일별 할일 목록을 조회합니다. startDate만 있으면 일별, startDate+endDate는 기간별, 둘 다 없으면 전체 조회")
	@GetMapping
	public ResponseEntity<CommonResponse<List<GroupedTodosResponseDto>>> getTodos(
			@Parameter(description = "멘티 ID 목록", example = "2,3") @RequestParam(value = "menteeId") List<Long> menteeIds,
			@Parameter(description = "시작 날짜 (YYYY-MM-DD), startDate만 있으면 일별, startDate+endDate는 기간별, 둘 다 없으면 전체 조회", example = "2026-02-01") @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@Parameter(description = "종료 날짜 (YYYY-MM-DD)", example = "2026-02-20") @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		List<GroupedTodosResponseDto> todos = todoService.findTodos(menteeIds, startDate, endDate);
		return ResponseEntity.ok(CommonResponse.of(todos));
	}

	@Operation(summary = "주차별 할일 목록 조회", description = "특정 주차의 월요일 날짜를 기준으로 해당 주(월~일)의 할일 목록을 조회")
	@GetMapping("/weekly")
	public ResponseEntity<CommonResponse<List<GroupedTodosResponseDto>>> getTodosWeekly(
			@Parameter(description = "멘티 ID 목록", example = "2,3") @RequestParam(value = "menteeId") List<Long> menteeIds,
			@Parameter(description = "주차의 월요일 날짜 (YYYY-MM-DD)", example = "2026-02-02") @RequestParam(value = "mondayDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mondayDate) {

		List<GroupedTodosResponseDto> weeklyTodos = todoService.findTodosWeekly(menteeIds, mondayDate);
		return ResponseEntity.ok(CommonResponse.of(weeklyTodos));
	}

	@Operation(summary = "할일 상세 조회", description = "할일 ID를 기반으로 상세 정보를 조회합니다.")
	@GetMapping("/{todoId}")
	public ResponseEntity<CommonResponse<TodoDetailResponseDto>> getTodoDetail(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId) {
		TodoDetailResponseDto detail = todoService.findTodoDetail(todoId);
		return ResponseEntity.ok(CommonResponse.of(detail));
	}

	@Operation(summary = "할일 수정", description = "기존 할일 정보를 수정합니다.")
	@PutMapping("/{todoId}")
	public ResponseEntity<CommonResponse<TodoCreateResponseDto>> updateTodo(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId,
			@Parameter(description = "수정할 할일 정보", required = true) @RequestBody TodoCreateRequestDto request) {
		TodoCreateResponseDto updated = todoService.updateTodo(todoId, request);
		return ResponseEntity.ok(CommonResponse.of(updated));
	}

	@Operation(summary = "할일 삭제", description = "할일을 삭제합니다.")
	@DeleteMapping("/{todoId}")
	public ResponseEntity<CommonResponse<Void>> deleteTodo(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId) {
		todoService.deleteTodo(todoId);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "할일 등록", description = "새로운 할일을 등록합니다. 과목(subject)은 KOREAN, ENGLISH, MATH 중 하나여야 합니다.")
	@PostMapping
	public ResponseEntity<CommonResponse<TodoCreateResponseDto>> createTodo(
			@Parameter(description = "새로운 할일 정보", required = true) @RequestBody TodoCreateRequestDto request) {
		TodoCreateResponseDto created = todoService.createTodo(request);
		return ResponseEntity.ok(CommonResponse.of(created));
	}

	@Operation(summary = "할일 임시 저장", description = "할일을 임시 저장합니다. 과목(subject)은 KOREAN, ENGLISH, MATH 중 하나여야 합니다.")
	@PostMapping("/drafts")
	public ResponseEntity<CommonResponse<TodoCreateResponseDto>> createDraftTodo(
			@Parameter(description = "임시 저장할 할일 정보", required = true) @RequestBody TodoCreateRequestDto request) {
		TodoCreateResponseDto created = todoService.createDraftTodo(request);
		return ResponseEntity.ok(CommonResponse.of(created));
	}

	@Operation(summary = "할일 임시 저장 목록 조회", description = "임시 저장된 할일 목록을 조회합니다.")
	@GetMapping("/drafts")
	public ResponseEntity<CommonResponse<List<GroupedTodosResponseDto>>> getDraftTodos(
			@Parameter(description = "멘티 ID", example = "2") @RequestParam(value = "menteeId") Long menteeId) {

		List<GroupedTodosResponseDto> todos = todoService.findDraftTodos(menteeId);
		return ResponseEntity.ok(CommonResponse.of(todos));
	}

	@Operation(summary = "할일 임시 저장 삭제", description = "임시 저장된 할일을 삭제합니다.")
	@DeleteMapping("/drafts/{todoId}")
	public ResponseEntity<CommonResponse<Void>> deleteDraftTodo(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId) {

		todoService.deleteDraftTodo(todoId);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "할일 완료 상태 변경", description = "할일의 완료 상태를 토글합니다.")
	@PatchMapping("/{todoId}/completed")
	public ResponseEntity<CommonResponse<Void>> toggleStatus(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId) {
		todoService.markComplete(todoId);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "할일 학습지 파일 업로드", description = "할일에 대한 학습지/파일을 업로드합니다.")
	@PostMapping(value = "/{todoId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<VerificationResponseDto>> uploadTodoFile(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId,
			@Parameter(description = "학습지 파일 (여러 개 가능)", required = true) @RequestPart("files") List<MultipartFile> files) {

		VerificationResponseDto response = todoService.uploadTodoFile(todoId, files);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "할일 인증 사진 업로드", description = "할일에 대한 공부 인증 사진을 업로드합니다.")
	@PostMapping(value = "/{todoId}/verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<VerificationResponseDto>> uploadVerification(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId,
			@Parameter(description = "인증 사진 파일 (여러 개 가능)", required = true) @RequestPart("images") List<MultipartFile> images) {

		VerificationResponseDto response = todoService.uploadVerification(todoId, images);
		feedbackService.analyzeTodoImagesAsync(todoId);
		return ResponseEntity.ok(CommonResponse.of(response));
	}


    @Operation(summary = "할일 타이머 세션 추가", description = "Todo에 타이머 세션(시작~종료)을 추가로 저장합니다.")
    @PostMapping("/{todoId}/timer/sessions")
    public ResponseEntity<CommonResponse<Void>> addTimerSession(
            @PathVariable Long todoId,
            @RequestBody TodoTimerSessionCreateRequestDto request
    ) {  todoService.addTimerSession(todoId, request);
        return ResponseEntity.ok(CommonResponse.of(null));
    }

	@Operation(summary = "할일 인증 사진 수정", description = "기존 인증 사진을 새 사진으로 수정")
	@PutMapping(value = "/{todoId}/verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<VerificationResponseDto>> updateVerification(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId,
			@Parameter(description = "새로운 인증 사진 파일 (여러 개 가능)", required = true) @RequestPart("images") List<MultipartFile> images) {

		VerificationResponseDto response = todoService.updateVerification(todoId, images);
		feedbackService.analyzeTodoImagesAsync(todoId);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "할일 인증 사진 삭제", description = "할일에 등록된 인증 사진을 삭제하고, 할일 상태를 미완료로 변경")
	@DeleteMapping("/{todoId}/verification")
	public ResponseEntity<CommonResponse<VerificationResponseDto>> deleteVerification(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId) {

		VerificationResponseDto response = todoService.deleteVerificationImage(todoId);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "학습지/파일 다운로드", description = "제공된 파일 URL을 통해 파일을 다운로드")
	@GetMapping("/download")
	public ResponseEntity<Resource> downloadFile(
			@Parameter(description = "파일 URL (예: /solutions/국어/1/file1.pdf)", required = true) @RequestParam("fileUrl") String fileUrl) throws IOException {

		Resource resource = fileStorageService.loadFileAsResource(fileUrl);
		String contentType = fileStorageService.getContentType(resource);

		String filename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(resource);
	}

    @Operation(summary = "일별 타이머 조회", description = "특정 날짜의 모든 todo_timer를 모아 총시간/평균시간과 함께 반환합니다.")
    @GetMapping("/timers/daily")
    public ResponseEntity<CommonResponse<TodoTimerDailyResponseDto>> getTimersByDate(
            @Parameter(
                    description = "멘티 ID",
                    example = "2",
                    required = true
            )
            @RequestParam("menteeId") Long menteeId,

            @Parameter(
                    description = "조회할 날짜 (YYYY-MM-DD)",
                    example = "2026-02-03",
                    required = true
            )
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        TodoTimerDailyResponseDto result = todoService.getTimersByDate(menteeId, date);
        return ResponseEntity.ok(CommonResponse.of(result));
    }

}
