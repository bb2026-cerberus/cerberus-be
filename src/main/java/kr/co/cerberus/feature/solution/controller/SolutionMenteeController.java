package kr.co.cerberus.feature.solution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.solution.dto.SolutionResponseDto;
import kr.co.cerberus.feature.solution.service.SolutionService;
import kr.co.cerberus.global.common.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Mentee Weakness Solution Management", description = "멘티의 약점 맞춤 솔루션 관리 API")
@RestController
@RequestMapping("/api/mentees/weakness-solutions")
public class SolutionMenteeController {

    private final SolutionService solutionService;

	public SolutionMenteeController(SolutionService solutionService) {
		this.solutionService = solutionService;
	}

    @Operation(summary = "멘티의 약점 솔루션 목록 조회", description = "멘티의 솔루션 목록을 조회합니다.")
    @GetMapping()
    public ResponseEntity<CommonResponse<List<SolutionResponseDto>>> getWeaknessSolutionsByMentee(
            @RequestParam(value = "menteeId") Long menteeId
    ) {
        List<SolutionResponseDto> response = solutionService.getWeaknessSolutionsByMentee(menteeId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
