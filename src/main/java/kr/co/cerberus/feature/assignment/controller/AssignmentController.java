package kr.co.cerberus.feature.assignment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.assignment.dto.AssignmentDetailResponseDto;
import kr.co.cerberus.feature.assignment.dto.GroupedAssignmentsResponseDto;
import kr.co.cerberus.feature.assignment.service.AssignmentService;
import kr.co.cerberus.feature.todo.dto.VerificationResponseDto;
import kr.co.cerberus.global.common.CommonResponse;
import kr.co.cerberus.global.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "Assignment", description = "과제 관련 API")
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

	private final AssignmentService assignmentService;
	private final FileStorageService fileStorageService;

	@Operation(summary = "과제 목록 조회", description = "전체/기간별/일별 과제 목록을 조회합니다. startDate만 있으면 일별, startDate+endDate는 기간별, 둘 다 없으면 전체 조회")
	@GetMapping
	public ResponseEntity<CommonResponse<List<GroupedAssignmentsResponseDto>>> getAssignments(
			@Parameter(description = "멘티 ID", example = "2") @RequestParam(value = "menteeId") Long menteeId,
			@Parameter(description = "시작 날짜 (YYYY-MM-DD), startDate만 있으면 일별, startDate+endDate는 기간별, 둘 다 없으면 전체 조회", example = "2026-02-01") @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@Parameter(description = "종료 날짜 (YYYY-MM-DD)", example = "2026-02-20") @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		List<GroupedAssignmentsResponseDto> assignments = assignmentService.findAssignments(menteeId, startDate, endDate);
		return ResponseEntity.ok(CommonResponse.of(assignments));
	}

	@Operation(summary = "주차별 과제 목록 조회", description = "특정 주차의 월요일 날짜를 기준으로 해당 주(월~일)의 과제 목록을 조회")
	@GetMapping("/weekly")
	public ResponseEntity<CommonResponse<List<GroupedAssignmentsResponseDto>>> getAssignmentsWeekly(
			@Parameter(description = "멘티 ID", example = "2") @RequestParam(value = "menteeId") Long menteeId,
			@Parameter(description = "주차의 월요일 날짜 (YYYY-MM-DD)", example = "2026-02-02") @RequestParam(value = "mondayDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mondayDate) {

		List<GroupedAssignmentsResponseDto> weeklyAssignments = assignmentService.findAssignmentsWeekly(menteeId, mondayDate);
		return ResponseEntity.ok(CommonResponse.of(weeklyAssignments));
	}

	@Operation(summary = "과제 상세 조회", description = "과제 ID를 기반으로 상세 정보를 조회")
	@GetMapping("/{assignmentId}")
	public ResponseEntity<CommonResponse<AssignmentDetailResponseDto>> getAssignmentDetail(@Parameter(description = "과제 ID", example = "1") @PathVariable(name = "assignmentId") Long assignmentId) {
		AssignmentDetailResponseDto detail = assignmentService.findAssignmentDetail(assignmentId);
		return ResponseEntity.ok(CommonResponse.of(detail));
	}

	@Operation(summary = "과제 인증 사진 업로드", description = "과제에 대한 공부 인증 사진을 업로드합니다. 업로드 시 자동으로 완료 상태로 전환")
	@PostMapping(value = "/{assignmentId}/verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<VerificationResponseDto>> uploadVerification(
			@Parameter(description = "과제 ID", example = "1") @PathVariable(name = "assignmentId") Long assignmentId,
			@Parameter(description = "인증 사진 파일 (여러 개 가능)", required = true) @RequestPart("images") List<MultipartFile> images) {

		VerificationResponseDto response = assignmentService.uploadVerification(assignmentId, images);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "과제 인증 사진 수정", description = "기존 인증 사진을 새 사진으로 수정")
	@PutMapping(value = "/{assignmentId}/verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<VerificationResponseDto>> updateVerification(
			@Parameter(description = "과제 ID", example = "1") @PathVariable(name = "assignmentId") Long assignmentId,
			@Parameter(description = "새로운 인증 사진 파일 (여러 개 가능)", required = true) @RequestPart("images") List<MultipartFile> images) {

		VerificationResponseDto response = assignmentService.updateVerification(assignmentId, images);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "과제 인증 사진 삭제", description = "과제에 등록된 인증 사진을 삭제하고, 과제 상태를 미완료로 변경")
	@DeleteMapping("/{assignmentId}/verification")
	public ResponseEntity<CommonResponse<VerificationResponseDto>> deleteVerification(
			@Parameter(description = "과제 ID", example = "1") @PathVariable(name = "assignmentId") Long assignmentId) {

		VerificationResponseDto response = assignmentService.deleteVerificationImage(assignmentId);
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
}
