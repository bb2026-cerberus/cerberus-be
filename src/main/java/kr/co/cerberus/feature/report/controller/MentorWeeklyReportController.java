package kr.co.cerberus.feature.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.report.dto.WeeklyMenteeReportResponseDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportCreateRequestDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportResponseDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportUpdateRequestDto;
import kr.co.cerberus.feature.report.service.WeeklyReportService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Mentor Weekly Report Management", description = "멘토의 주간 리포트 관리 API")
@RestController
@RequestMapping("/api/mentors/weekly-reports")
@RequiredArgsConstructor
public class MentorWeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @Operation(summary = "주간 리포트 생성", description = "멘토가 멘티에 대한 주간 리포트를 생성합니다.")
    @PostMapping("/{mentorId}")
    public ResponseEntity<CommonResponse<WeeklyReportResponseDto>> createWeeklyReport(
            @PathVariable Long mentorId,
            @Valid @RequestBody WeeklyReportCreateRequestDto requestDto) {
        WeeklyReportResponseDto response = weeklyReportService.createWeeklyReport(mentorId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.of(response));
    }

    @Operation(summary = "주간 리포트 수정", description = "멘토가 등록된 주간 리포트를 수정합니다.")
    @PutMapping("/{mentorId}")
    public ResponseEntity<CommonResponse<WeeklyReportResponseDto>> updateWeeklyReport(
            @PathVariable Long mentorId,
            @Valid @RequestBody WeeklyReportUpdateRequestDto requestDto) {
        WeeklyReportResponseDto response = weeklyReportService.updateWeeklyReport(mentorId, requestDto);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "주간 리포트 삭제 (비활성화)", description = "주간 리포트를 비활성화합니다.")
    @DeleteMapping("/{mentorId}/{reportId}")
    public ResponseEntity<CommonResponse<Void>> deleteWeeklyReport(
            @PathVariable Long mentorId,
            @PathVariable Long reportId) {
        weeklyReportService.deleteWeeklyReport(mentorId, reportId);
        return ResponseEntity.ok(CommonResponse.of(null));
    }

    @Operation(summary = "주간 리포트 상세 조회", description = "특정 주간 리포트의 상세 정보를 조회합니다.")
    @GetMapping("/{reportId}")
    public ResponseEntity<CommonResponse<WeeklyReportResponseDto>> getWeeklyReportDetail(@PathVariable Long reportId) {
        WeeklyReportResponseDto response = weeklyReportService.getWeeklyReportDetail(reportId);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘토가 관리하는 모든 멘티의 주간 리포트 목록 조회", description = "특정 주차(월요일 기준)의 모든 멘티 리포트를 조회합니다. 작성 전인 경우 AI 초안이 포함됩니다.")
    @GetMapping("/by-mentor/{mentorId}")
    public ResponseEntity<CommonResponse<List<WeeklyReportResponseDto>>> getWeeklyReportsByMentor(
            @PathVariable Long mentorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mondayDate) {
        List<WeeklyReportResponseDto> response = weeklyReportService.getMenteesWeeklyReports(mentorId, mondayDate);
        return ResponseEntity.ok(CommonResponse.of(response));
    }

    @Operation(summary = "멘티의 주간 리포트 목록 조회 (멘티용)", description = "멘티가 자신의 특정 주간 리포트 목록을 조회합니다. yearMonthWeek는 2026년 2월 1주차면 20260201 형식으로 전달")
    @GetMapping("/by-mentee/{menteeId}/week/{yearMonthWeek}")
    public ResponseEntity<CommonResponse<WeeklyMenteeReportResponseDto>> getWeeklyReportsByMentee(
            @PathVariable Long menteeId, @PathVariable String yearMonthWeek) {
        WeeklyMenteeReportResponseDto response = weeklyReportService.getWeeklyReportsByMentee(menteeId, yearMonthWeek);
        return ResponseEntity.ok(CommonResponse.of(response));
    }
}
