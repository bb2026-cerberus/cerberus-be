package kr.co.cerberus.feature.weakness.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.weakness.dto.WeaknessSolutionCreateRequestDto;
import kr.co.cerberus.feature.weakness.dto.WeaknessSolutionResponseDto;
import kr.co.cerberus.feature.weakness.dto.WeaknessSolutionUpdateRequestDto;
import kr.co.cerberus.feature.weakness.service.WeaknessSolutionService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Mentor Weakness Solution Management", description = "멘토의 약점 맞춤 솔루션 관리 API")
@RestController
@RequestMapping("/api/mentors/weakness-solutions")
@RequiredArgsConstructor
public class MentorWeaknessSolutionController {

    private final WeaknessSolutionService weaknessSolutionService;

    @Operation(summary = "약점 솔루션 생성", description = "멘토가 멘티에 대한 약점 맞춤 솔루션을 생성합니다.")
    @PostMapping("/{mentorId}")
    public ResponseEntity<CommonResponse<WeaknessSolutionResponseDto>> createWeaknessSolution(
            @PathVariable Long mentorId,
            @Valid @RequestBody WeaknessSolutionCreateRequestDto requestDto) {
        WeaknessSolutionResponseDto response = weaknessSolutionService.createWeaknessSolution(mentorId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.of(response));
    }

    @Operation(summary = "약점 솔루션 수정", description = "멘토가 등록된 약점 맞춤 솔루션을 수정합니다.")
    @PutMapping("/{mentorId}")
    public ResponseEntity<CommonResponse<WeaknessSolutionResponseDto>> updateWeaknessSolution(
            @PathVariable Long mentorId,
            @Valid @RequestBody WeaknessSolutionUpdateRequestDto requestDto) {
        WeaknessSolutionResponseDto response = weaknessSolutionService.updateWeaknessSolution(mentorId, requestDto);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "약점 솔루션 삭제 (비활성화)", description = "약점 맞춤 솔루션을 비활성화합니다.")
    @DeleteMapping("/{mentorId}/{weaknessSolutionId}")
    public ResponseEntity<CommonResponse<Void>> deleteWeaknessSolution(
            @PathVariable Long mentorId,
            @PathVariable Long weaknessSolutionId) {
        weaknessSolutionService.deleteWeaknessSolution(mentorId, weaknessSolutionId);
        return ResponseEntity.ok(CommonResponse.of(null));
    }

    @Operation(summary = "약점 솔루션 상세 조회", description = "특정 약점 맞춤 솔루션의 상세 정보를 조회합니다.")
    @GetMapping("/{weaknessSolutionId}")
    public ResponseEntity<CommonResponse<WeaknessSolutionResponseDto>> getWeaknessSolutionDetail(@PathVariable Long weaknessSolutionId) {
        WeaknessSolutionResponseDto response = weaknessSolutionService.getWeaknessSolutionDetail(weaknessSolutionId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘토가 특정 멘티의 약점 솔루션 목록 조회", description = "멘토가 관리하는 특정 멘티의 약점 맞춤 솔루션 목록을 조회합니다.")
    @GetMapping("/by-mentor/{mentorId}/mentees/{menteeId}")
    public ResponseEntity<CommonResponse<List<WeaknessSolutionResponseDto>>> getWeaknessSolutionsByMentorAndMentee(
            @PathVariable Long mentorId,
            @PathVariable Long menteeId) {
        List<WeaknessSolutionResponseDto> response = weaknessSolutionService.getWeaknessSolutionsByMentorAndMentee(mentorId, menteeId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘티가 자신의 약점 솔루션 목록 조회 (멘티용)", description = "멘티가 자신의 약점 맞춤 솔루션 목록을 조회합니다.")
    @GetMapping("/by-mentee/{menteeId}")
    public ResponseEntity<CommonResponse<List<WeaknessSolutionResponseDto>>> getWeaknessSolutionsByMentee(
            @PathVariable Long menteeId) {
        List<WeaknessSolutionResponseDto> response = weaknessSolutionService.getWeaknessSolutionsByMentee(menteeId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
