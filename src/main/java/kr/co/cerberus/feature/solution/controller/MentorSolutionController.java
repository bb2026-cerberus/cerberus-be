package kr.co.cerberus.feature.solution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.solution.dto.SolutionCreateRequestDto;
import kr.co.cerberus.feature.solution.dto.SolutionResponseDto;
import kr.co.cerberus.feature.solution.dto.SolutionUpdateRequestDto;
import kr.co.cerberus.feature.solution.service.SolutionService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Mentor Solution Management", description = "멘토의 솔루션(학습지) 관리 API")
@RestController
@RequestMapping("/api/mentors/solutions")
@RequiredArgsConstructor
public class MentorSolutionController {

    private final SolutionService solutionService;

    @Operation(summary = "새 솔루션 등록", description = "멘토가 새로운 학습지 솔루션을 등록합니다.")
    @PostMapping
    public ResponseEntity<CommonResponse<SolutionResponseDto>> createSolution(
            @Valid @RequestBody SolutionCreateRequestDto requestDto) {
        SolutionResponseDto response = solutionService.createSolution(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.of(response));
    }

    @Operation(summary = "솔루션 정보 수정", description = "멘토가 등록된 학습지 솔루션의 정보를 수정합니다. Path Variable의 mentorId는 인증된 멘토의 ID로 사용됩니다.")
    @PutMapping("/{mentorId}")
    public ResponseEntity<CommonResponse<SolutionResponseDto>> updateSolution(
            @PathVariable Long mentorId,
            @Valid @RequestBody SolutionUpdateRequestDto requestDto) {
        SolutionResponseDto response = solutionService.updateSolution(mentorId, requestDto);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "솔루션 삭제 (비활성화)", description = "멘토가 등록된 학습지 솔루션을 비활성화합니다. Path Variable의 mentorId는 인증된 멘토의 ID로 사용됩니다.")
    @DeleteMapping("/{mentorId}/{solutionId}")
    public ResponseEntity<CommonResponse<Void>> deleteSolution(
            @PathVariable Long mentorId,
            @PathVariable Long solutionId) {
        solutionService.deleteSolution(mentorId, solutionId);
        return ResponseEntity.ok(CommonResponse.of(null));
    }

    @Operation(summary = "솔루션 상세 조회", description = "특정 솔루션의 상세 정보를 조회합니다.")
    @GetMapping("/{solutionId}")
    public ResponseEntity<CommonResponse<SolutionResponseDto>> getSolutionDetail(@PathVariable Long solutionId) {
        SolutionResponseDto response = solutionService.getSolutionDetail(solutionId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘토별 솔루션 목록 조회", description = "멘토가 등록한 모든 학습지 솔루션 목록을 조회합니다.")
    @GetMapping("/by-mentor/{mentorId}")
    public ResponseEntity<CommonResponse<List<SolutionResponseDto>>> getSolutionsByMentor(@PathVariable Long mentorId) {
        List<SolutionResponseDto> response = solutionService.getSolutionsByMentor(mentorId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘토별 솔루션 검색", description = "멘토가 등록한 학습지 솔루션을 제목으로 검색합니다.")
    @GetMapping("/by-mentor/{mentorId}/search")
    public ResponseEntity<CommonResponse<List<SolutionResponseDto>>> searchSolutionsByTitle(
            @PathVariable Long mentorId,
            @RequestParam String title) {
        List<SolutionResponseDto> response = solutionService.searchSolutionsByTitle(mentorId, title);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
