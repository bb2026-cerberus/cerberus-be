package kr.co.cerberus.feature.todo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.todo.dto.*;
import kr.co.cerberus.feature.todo.service.TodoService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Todo", description = "할일 관련 API")
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

	private final TodoService todoService;

	@Operation(summary = "할일 목록 조회", description = "전체/기간별/일별 할일 목록을 조회합니다. startDate만 있으면 일별, startDate+endDate는 기간별, 둘 다 없으면 전체 조회")
	@GetMapping
	public ResponseEntity<CommonResponse<List<GroupedTodosResponseDto>>> getTodos(
			@Parameter(description = "멘티 ID", example = "2") @RequestParam(value = "menteeId") Long menteeId,
			@Parameter(description = "시작 날짜 (YYYY-MM-DD), startDate만 있으면 일별, startDate+endDate는 기간별, 둘 다 없으면 전체 조회", example = "2026-02-01") @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@Parameter(description = "종료 날짜 (YYYY-MM-DD)", example = "2026-02-20") @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		List<GroupedTodosResponseDto> todos = todoService.findTodos(menteeId, startDate, endDate);
		return ResponseEntity.ok(CommonResponse.of(todos));
	}

	@Operation(summary = "주차별 할일 목록 조회", description = "특정 주차의 월요일 날짜를 기준으로 해당 주(월~일)의 할일 목록을 조회")
	@GetMapping("/weekly")
	public ResponseEntity<CommonResponse<List<GroupedTodosResponseDto>>> getTodosWeekly(
			@Parameter(description = "멘티 ID", example = "2") @RequestParam(value = "menteeId") Long menteeId,
			@Parameter(description = "주차의 월요일 날짜 (YYYY-MM-DD)", example = "2026-02-02") @RequestParam(value = "mondayDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mondayDate) {

		List<GroupedTodosResponseDto> weeklyTodos = todoService.findTodosWeekly(menteeId, mondayDate);
		return ResponseEntity.ok(CommonResponse.of(weeklyTodos));
	}

	@Operation(summary = "할일 상세 조회", description = "할일 ID를 기반으로 상세 정보를 조회합니다.")
	@GetMapping("/{todoId}")
	public ResponseEntity<CommonResponse<TodoDetailResponseDto>> getTodoDetail(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId) {
		TodoDetailResponseDto detail = todoService.findTodoDetail(todoId);
		return ResponseEntity.ok(CommonResponse.of(detail));
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

	@Operation(summary = "할일 완료 상태 변경", description = "할일의 완료 상태를 토글합니다.")
	@PatchMapping("/{todoId}/completed")
	public ResponseEntity<CommonResponse<Void>> toggleStatus(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId) {
		todoService.toggleStatus(todoId);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "할일 인증 사진 업로드", description = "할일에 대한 공부 인증 사진을 업로드합니다.")
	@PostMapping(value = "/{todoId}/verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<VerificationResponseDto>> uploadVerification(
			@Parameter(description = "할일 ID", example = "1") @PathVariable(name = "todoId") Long todoId,
			@Parameter(description = "인증 사진 파일 (여러 개 가능)", required = true) @RequestPart("images") List<MultipartFile> images) {

		VerificationResponseDto response = todoService.uploadVerification(todoId, images);
		return ResponseEntity.ok(CommonResponse.of(response));
	}
}
