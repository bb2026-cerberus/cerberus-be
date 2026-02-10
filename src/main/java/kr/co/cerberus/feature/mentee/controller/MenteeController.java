package kr.co.cerberus.feature.mentee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.mentee.dto.MenteeMypageResponseDto;
import kr.co.cerberus.feature.mentee.service.MenteeService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Mentee", description = "멘티 기능 API")
@RestController
@RequestMapping("/api/mentees")
@RequiredArgsConstructor
public class MenteeController {

    private final MenteeService menteeService;

    @Operation(summary = "멘티 마이페이지", description = "멘티 마이페아지 - 이름, 이번 주 달성률, 과목별 진행도, 최근 피드백 요약을 조회합니다.")
    @GetMapping("/{menteeId}/my-page")
    public ResponseEntity<CommonResponse<MenteeMypageResponseDto>> getMenteeDetails(
            @PathVariable Long menteeId) {
        MenteeMypageResponseDto response = menteeService.getMenteeMypage(menteeId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
