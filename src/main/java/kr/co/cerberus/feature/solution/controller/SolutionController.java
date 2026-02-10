package kr.co.cerberus.feature.solution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.solution.dto.SolutionCreateRequestDto;
import kr.co.cerberus.feature.solution.dto.SolutionResponseDto;
import kr.co.cerberus.feature.solution.dto.SolutionUpdateRequestDto;
import kr.co.cerberus.feature.solution.service.SolutionService;
import kr.co.cerberus.global.common.CommonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Weakness Solution Management", description = "약점 맞춤 솔루션 관리 API")
@RestController
@RequestMapping("/api/mentors/weakness-solutions")
public class SolutionController {

    private final SolutionService solutionService;
	
	public SolutionController(SolutionService solutionService) {
		this.solutionService = solutionService;
	}
	
	@Operation(summary = "약점 솔루션 생성", description = "멘토가 멘티에 대한 약점 맞춤 솔루션을 생성합니다.")
    @PostMapping(value = "/{mentorId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<SolutionResponseDto>> createWeaknessSolution(
            @PathVariable Long mentorId,
            @Valid @RequestPart("request") SolutionCreateRequestDto requestDto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        SolutionResponseDto response = solutionService.createSolution(mentorId, requestDto, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.of(response));
    }

    @Operation(summary = "약점 솔루션 수정", description = "멘토가 등록된 약점 맞춤 솔루션을 수정합니다.")
    @PutMapping(value = "/{mentorId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<SolutionResponseDto>> updateWeaknessSolution(
            @PathVariable Long mentorId,
            @Valid @RequestPart("request") SolutionUpdateRequestDto requestDto,
            @RequestPart(value = "files", required = false) MultipartFile file) {
        SolutionResponseDto response = solutionService.updateSolution(mentorId, requestDto, file);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "약점 솔루션 삭제 (비활성화)", description = "약점 맞춤 솔루션을 비활성화합니다.")
    @DeleteMapping("/{mentorId}/{weaknessSolutionId}")
    public ResponseEntity<CommonResponse<Void>> deleteWeaknessSolution(
            @PathVariable Long mentorId,
            @PathVariable Long weaknessSolutionId) {
        solutionService.deleteWeaknessSolution(mentorId, weaknessSolutionId);
        return ResponseEntity.ok(CommonResponse.of(null));
    }

    @Operation(summary = "멘토의 약점 솔루션 목록 조회", description = "멘토가 관리하는 특정 멘티의 솔루션 목록을 조회하거나, menteeId가 없으면 멘토가 등록한 모든 솔루션을 조회합니다.")
    @GetMapping("/by-mentor/{mentorId}")
    public ResponseEntity<CommonResponse<List<SolutionResponseDto>>> getWeaknessSolutionsByMentor(
            @PathVariable Long mentorId,
            @RequestParam(value = "menteeId", required = false) Long menteeId) {
        List<SolutionResponseDto> response = solutionService.getWeaknessSolutionsByMentorAndMentee(mentorId, menteeId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘티의 약점 솔루션 목록 조회", description = "특정 멘티의 솔루션 목록을 조회")
    @GetMapping("/by-mentee/{mentorId}")
    public ResponseEntity<CommonResponse<List<SolutionResponseDto>>> getWeaknessSolutionsByMentor(
            @PathVariable Long menteeId) {
        List<SolutionResponseDto> response = solutionService.getWeaknessSolutionsByMentee(menteeId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
