package kr.co.cerberus.feature.mentor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.mentor.dto.DraftCountResponseDto;
import kr.co.cerberus.feature.mentor.dto.MenteeDetailsResponseDto;
import kr.co.cerberus.feature.mentor.dto.MenteeListResponseDto;
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

    @Operation(summary = "담당 멘티 목록 조회", description = "멘토가 담당하는 멘티 목록을 조회합니다.")
    @GetMapping("/{mentorId}/mentees")
    public ResponseEntity<CommonResponse<MenteeListResponseDto>> getMenteeList(
            @PathVariable Long mentorId) {
        MenteeListResponseDto response = mentorService.getMenteeList(mentorId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
