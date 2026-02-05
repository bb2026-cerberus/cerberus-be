package kr.co.cerberus.feature.planner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.planner.dto.PlannerImageResponseDto;
import kr.co.cerberus.feature.planner.dto.PlannerQuestionRequestDto;
import kr.co.cerberus.feature.planner.dto.PlannerResponseDto;
import kr.co.cerberus.feature.planner.service.PlannerService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Planner", description = "플래너 관련 API")
@RestController
@RequestMapping("/api/planners")
@RequiredArgsConstructor
public class PlannerController {

	private final PlannerService plannerService;

	@Operation(summary = "플래너 조회", description = "특정 멘티의 특정 날짜 플래너를 조회합니다.")
	@GetMapping
	public ResponseEntity<CommonResponse<PlannerResponseDto>> getPlanner(
			@RequestParam Long menteeId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		PlannerResponseDto planner = plannerService.findPlanner(menteeId, date);
		return ResponseEntity.ok(CommonResponse.of(planner));
	}

	@Operation(summary = "플래너 이미지 업로드", description = "플래너에 이미지를 업로드합니다.")
	@PostMapping("/image")
	public ResponseEntity<CommonResponse<PlannerImageResponseDto>> uploadImage(
			@RequestPart("images") List<MultipartFile> images,
			@RequestPart("menteeId") Long menteeId,
			@RequestPart("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		PlannerImageResponseDto response = plannerService.uploadImage(menteeId, date, images);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "질문 등록", description = "플래너에 질문을 등록합니다.")
	@PostMapping("/question")
	public ResponseEntity<CommonResponse<Void>> registerQuestion(@RequestBody PlannerQuestionRequestDto request) {
		plannerService.registerQuestion(request);
		return ResponseEntity.ok(CommonResponse.of(null));
	}
}
