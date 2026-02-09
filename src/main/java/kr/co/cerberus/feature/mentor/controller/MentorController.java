package kr.co.cerberus.feature.mentor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.mentor.dto.DraftCountResponseDto;
import kr.co.cerberus.feature.mentor.dto.MenteeProgressResponseDto;
import kr.co.cerberus.feature.mentor.dto.MentorHomeResponseDto;
import kr.co.cerberus.feature.mentor.service.MentorService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Mentor", description = "멘토 기능 API")
@RestController
@RequestMapping("/api/mentors")
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    @Operation(summary = "멘토 홈 화면 데이터 조회", description = "멘토 ID와 날짜 기반으로 과제, 피드백, Q&A 현황을 조회합니다.")
    @GetMapping("/{mentorId}/home")
    public ResponseEntity<CommonResponse<MentorHomeResponseDto>> getMentorHomeData(
            @PathVariable Long mentorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MentorHomeResponseDto response = mentorService.getMentorHomeData(mentorId, date);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘토의 임시저장 개수 조회", description = "멘토가 관리하는 멘티들의 과제 및 피드백 임시저장 개수를 조회합니다.")
    @GetMapping("/{mentorId}/draft-counts")
    public ResponseEntity<CommonResponse<DraftCountResponseDto>> getDraftCounts(@PathVariable Long mentorId) {
        DraftCountResponseDto response = mentorService.getDraftCounts(mentorId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘티별 진행률 통계 조회", description = "멘토 ID와 멘티 ID 기반으로 멘티의 전체 및 과목별 과제 진행률을 조회합니다.")
    @GetMapping("/{mentorId}/mentees/{menteeId}/progress")
    public ResponseEntity<CommonResponse<MenteeProgressResponseDto>> getMenteeProgress(
            @PathVariable Long mentorId,
            @PathVariable Long menteeId) {
        MenteeProgressResponseDto response = mentorService.getMenteeProgress(mentorId, menteeId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘티 상세 현황 조회", description = "이미지 img_1.png 요구사항에 맞춰 오늘 현황, 이번 주 달성률, 과목별 진행도, 최근 피드백 요약을 조회합니다.")
    @GetMapping("/{mentorId}/mentees/{menteeId}/details")
    public ResponseEntity<CommonResponse<MenteeDetailsResponseDto>> getMenteeDetails(
            @PathVariable Long mentorId,
            @PathVariable Long menteeId) {
        MenteeDetailsResponseDto response = mentorService.getMenteeDetails(mentorId, menteeId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
