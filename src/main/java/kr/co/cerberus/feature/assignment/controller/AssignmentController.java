package kr.co.cerberus.feature.assignment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.assignment.dto.AssignmentDetailResponseDto;
import kr.co.cerberus.feature.assignment.dto.AssignmentListResponseDto;
import kr.co.cerberus.feature.assignment.service.AssignmentService;
import kr.co.cerberus.feature.todo.dto.VerificationResponseDto;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Assignment", description = "과제 관련 API")
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

	private final AssignmentService assignmentService;

	@Operation(summary = "과제 목록 조회", description = "전체/기간별/일별 과제 목록을 조회합니다. startDate만 있으면 일별, startDate+endDate는 기간별, 둘 다 없으면 전체 조회")
	@GetMapping
	public ResponseEntity<CommonResponse<List<AssignmentListResponseDto>>> getAssignments(
			@RequestParam Long menteeId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		List<AssignmentListResponseDto> assignments = assignmentService.findAssignments(menteeId, startDate, endDate);
		return ResponseEntity.ok(CommonResponse.of(assignments));
	}

	@Operation(summary = "과제 상세 조회", description = "과제 ID를 기반으로 상세 정보를 조회합니다.")
	@GetMapping("/{assignmentId}")
	public ResponseEntity<CommonResponse<AssignmentDetailResponseDto>> getAssignmentDetail(@PathVariable Long assignmentId) {
		AssignmentDetailResponseDto detail = assignmentService.findAssignmentDetail(assignmentId);
		return ResponseEntity.ok(CommonResponse.of(detail));
	}

	@Operation(summary = "과제 인증 사진 업로드", description = "과제에 대한 공부 인증 사진을 업로드합니다. 업로드 시 자동으로 완료 상태로 전환됩니다.")
	@PostMapping("/{assignmentId}/verification")
	public ResponseEntity<CommonResponse<VerificationResponseDto>> uploadVerification(
			@PathVariable Long assignmentId,
			@RequestPart("images") List<MultipartFile> images) {

		VerificationResponseDto response = assignmentService.uploadVerification(assignmentId, images);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "과제 인증 사진 수정", description = "기존 인증 사진을 새 사진으로 수정합니다.")
	@PutMapping("/{assignmentId}/verification")
	public ResponseEntity<CommonResponse<VerificationResponseDto>> updateVerification(
			@PathVariable Long assignmentId,
			@RequestPart("images") List<MultipartFile> images) {

		VerificationResponseDto response = assignmentService.updateVerification(assignmentId, images);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	// TODO: 학습지 PDF 다운로드 API
	@Operation(summary = "학습지 PDF 다운로드", description = "과제에 첨부된 학습지 PDF를 다운로드합니다.")
	@GetMapping("/{assignmentId}/workbook/{fileId}")
	public ResponseEntity<Void> downloadWorkbook(
			@PathVariable Long assignmentId,
			@PathVariable Long fileId) {
		// TODO: 파일 다운로드 로직 구현
		return ResponseEntity.ok().build();
	}
}
