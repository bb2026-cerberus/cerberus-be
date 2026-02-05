package kr.co.cerberus.feature.todo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.todo.dto.*;
import kr.co.cerberus.feature.todo.service.TodoService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
	public ResponseEntity<CommonResponse<List<TodoListResponseDto>>> getTodos(
			@RequestParam Long menteeId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		List<TodoListResponseDto> todos = todoService.findTodos(menteeId, startDate, endDate);
		return ResponseEntity.ok(CommonResponse.of(todos));
	}

	@Operation(summary = "할일 상세 조회", description = "할일 ID를 기반으로 상세 정보를 조회합니다.")
	@GetMapping("/{todoId}")
	public ResponseEntity<CommonResponse<TodoDetailResponseDto>> getTodoDetail(@PathVariable Long todoId) {
		TodoDetailResponseDto detail = todoService.findTodoDetail(todoId);
		return ResponseEntity.ok(CommonResponse.of(detail));
	}

	@Operation(summary = "할일 등록", description = "새로운 할일을 등록합니다.")
	@PostMapping
	public ResponseEntity<CommonResponse<TodoCreateResponseDto>> createTodo(@RequestBody TodoCreateRequestDto request) {
		TodoCreateResponseDto created = todoService.createTodo(request);
		return ResponseEntity.ok(CommonResponse.of(created));
	}

	@Operation(summary = "할일 완료 상태 변경", description = "할일의 완료 상태를 토글합니다.")
	@PatchMapping("/{todoId}/status")
	public ResponseEntity<CommonResponse<Void>> toggleStatus(@PathVariable Long todoId) {
		todoService.toggleStatus(todoId);
		return ResponseEntity.ok(CommonResponse.of(null));
	}

	@Operation(summary = "할일 인증 사진 업로드", description = "할일에 대한 공부 인증 사진을 업로드합니다.")
	@PostMapping("/{todoId}/verification")
	public ResponseEntity<CommonResponse<VerificationResponseDto>> uploadVerification(
			@PathVariable Long todoId,
			@RequestPart("images") List<MultipartFile> images) {

		VerificationResponseDto response = todoService.uploadVerification(todoId, images);
		return ResponseEntity.ok(CommonResponse.of(response));
	}
}
