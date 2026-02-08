package kr.co.cerberus.feature.report.service;

import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.report.WeeklyReport;
import kr.co.cerberus.feature.report.dto.WeeklyMenteeReportResponseDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportCreateRequestDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportResponseDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportUpdateRequestDto;
import kr.co.cerberus.feature.report.repository.WeeklyReportRepository;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final MemberRepository memberRepository; // 멘티/멘토 유효성 검증용
    private final RelationRepository relationRepository; // 멘토-멘티 관계 검증용
    private final TodoRepository todoRepository;
    // 주간 리포트 생성
    @Transactional
    public WeeklyReportResponseDto createWeeklyReport(Long mentorId, WeeklyReportCreateRequestDto requestDto) {
        // 요청하는 멘토와 리포트의 멘토 ID 일치 여부 확인 (파라미터로 받은 mentorId를 신뢰)
        if (!Objects.equals(requestDto.mentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 리포트만 생성할 수 있습니다.");
        }
        // 멘토 및 멘티 ID 유효성 및 관계 검증
        validateMentorMenteeRelation(mentorId, requestDto.menteeId());

        WeeklyReport report = WeeklyReport.builder()
                .menteeId(requestDto.menteeId())
                .mentorId(mentorId)
                .reportDate(requestDto.reportDate())
                .summary(requestDto.summary())
                .overallEvaluation(requestDto.overallEvaluation())
                .strengths(requestDto.strengths())
                .improvements(requestDto.improvements())
                .reportFile(JsonbUtils.toJson(requestDto.reportFiles()))
                .build();
        WeeklyReport savedReport = weeklyReportRepository.save(report);
        return mapToResponseDto(savedReport);
    }

    // 주간 리포트 수정
    @Transactional
    public WeeklyReportResponseDto updateWeeklyReport(Long mentorId, WeeklyReportUpdateRequestDto requestDto) {
        WeeklyReport report = weeklyReportRepository.findById(requestDto.reportId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 요청하는 멘토가 해당 리포트의 소유자인지 확인
        if (!Objects.equals(report.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 리포트를 수정할 권한이 없습니다.");
        }

        report.updateReport(
                requestDto.summary(),
                requestDto.overallEvaluation(),
                requestDto.strengths(),
                requestDto.improvements(),
                JsonbUtils.toJson(requestDto.reportFiles())
        );
        return mapToResponseDto(report);
    }

    // 주간 리포트 삭제 (비활성화)
    @Transactional
    public void deleteWeeklyReport(Long mentorId, Long reportId) {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 요청하는 멘토가 해당 리포트의 소유자인지 확인
        if (!Objects.equals(report.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 리포트를 삭제할 권한이 없습니다.");
        }
        report.delete();
    }

    // 주간 리포트 상세 조회
    public WeeklyReportResponseDto getWeeklyReportDetail(Long reportId) {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponseDto(report);
    }

    // 멘토가 특정 멘티의 주간 리포트 목록 조회 (기간별)
    public List<WeeklyReportResponseDto> getWeeklyReportsByMentorAndMentee(Long mentorId, Long menteeId, LocalDate startDate, LocalDate endDate) {
        // 멘토-멘티 관계 확인
        validateMentorMenteeRelation(mentorId, menteeId);

        List<WeeklyReport> reports = weeklyReportRepository.findByMentorIdAndMenteeIdAndReportDateBetweenAndActivateYn(
                mentorId, menteeId, startDate, endDate, "Y");
        return reports.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // 멘티의 주간 리포트 목록 조회
    public WeeklyMenteeReportResponseDto getWeeklyReportsByMentee(Long menteeId, String yearMonthWeek) {
//        yearMonthWeek 가 2026021이런식으로 2026년 2월의 1주차를 의미
        int year = Integer.parseInt(yearMonthWeek.substring(0, 4));
        int month = Integer.parseInt(yearMonthWeek.substring(4, 6));
        int week = Integer.parseInt(yearMonthWeek.substring(6, 7));

        LocalDate startDate = LocalDate.of(year, month, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)).plusWeeks(week - 1);
        LocalDate endDate = startDate.plusDays(6);
        System.out.println("startDate: " + startDate + ", endDate: " + endDate);
        WeeklyReport report = weeklyReportRepository.findByMenteeIdAndReportDateBetween(
                menteeId, startDate, endDate
        );
        System.out.println("report: " + report);


        // ✅ 이제 Todo 테이블에서 한 번에 주간 데이터 조회 (deleteYn만 필터)
        List<Todo> weeklyItems = todoRepository.findByMenteeIdAndTodoDateBetweenAndDeleteYn(
                menteeId, startDate, endDate, "N"
        );

        // ✅ todo_assign_yn 기준으로 분리
        List<Todo> assignments = weeklyItems.stream()
                .filter(t -> "Y".equals(t.getTodoAssignYn()))  // 과제
                .toList();

        List<Todo> todos = weeklyItems.stream()
                .filter(t -> "N".equals(t.getTodoAssignYn()))  // 할일
                .toList();

        // =========================
        // 과제 달성률
        // =========================
        int totalAssignCount = assignments.size();
        long completedAssignCount = assignments.stream()
                .filter(t -> "Y".equals(t.getTodoCompleteYn()))
                .count();

        int completedAssignPercent = 0;
        if (totalAssignCount > 0) {
            completedAssignPercent = (int) Math.round((double) completedAssignCount / totalAssignCount * 100);
        }

        // =========================
        // 할일 달성률
        // =========================
        int totalTodoCount = todos.size();
        long completedTodoCount = todos.stream()
                .filter(t -> "Y".equals(t.getTodoCompleteYn()))
                .count();

        int completedTodoPercent = 0;
        if (totalTodoCount > 0) {
            completedTodoPercent = (int) Math.round((double) completedTodoCount / totalTodoCount * 100);
        }

        // =========================
        // 과목별 달성률 (과제+할일 합산)
        // =========================
        List<String> subjects = List.of("국어", "영어", "수학");

        Map<String, long[]> subjectStats = new LinkedHashMap<>();
        for (String s : subjects) {
            subjectStats.put(s, new long[]{0L, 0L}); // [0]=total, [1]=completed
        }

        // ✅ 주간 항목 전체(과제+할일)에서 과목별 집계
        for (Todo t : weeklyItems) {
            String sub = t.getTodoSubjects();
            if (!subjectStats.containsKey(sub)) continue;

            subjectStats.get(sub)[0]++; // total++
            if ("Y".equals(t.getTodoCompleteYn())) {
                subjectStats.get(sub)[1]++; // completed++
            }
        }

        Map<String, Integer> subjectPercentMap = new LinkedHashMap<>();
        for (String sub : subjects) {
            long total = subjectStats.get(sub)[0];
            long completed = subjectStats.get(sub)[1];

            int percent = (total == 0) ? 0 : (int) Math.round((double) completed / total * 100);
            subjectPercentMap.put(sub, percent);
        }

        return new WeeklyMenteeReportResponseDto(
                report == null ? null : report.getId(),
                report == null ? null : report.getMentorId(),
                report == null ? null : report.getReportDate(),
                report == null ? null : report.getSummary(),
                report == null ? null : report.getOverallEvaluation(),
                report == null ? null : report.getStrengths(),
                report == null ? null : report.getImprovements(),
                report == null ? null : report.getCreateDatetime(),
                report == null ? null : report.getUpdateDatetime(),
                completedAssignPercent,
                completedTodoPercent,
                subjectPercentMap
        );
    }

    private WeeklyReportResponseDto mapToResponseDto(WeeklyReport report) {
        return new WeeklyReportResponseDto(
                report.getId(),
                report.getMenteeId(),
                report.getMentorId(),
                report.getReportDate(),
                report.getSummary(),
                report.getOverallEvaluation(),
                report.getStrengths(),
                report.getImprovements(),
                Optional.ofNullable(JsonbUtils.fromJson(report.getReportFile(), new TypeReference<List<kr.co.cerberus.global.jsonb.FileInfo>>() {})).orElse(List.of()),
                report.getCreateDatetime(),
                report.getUpdateDatetime()
        );
    }
    
    // 멘토와 멘티의 관계 유효성 검증
    private void validateMentorMenteeRelation(Long mentorId, Long menteeId) {
        if (!memberRepository.findById(mentorId).map(m -> m.getRole() == Role.MENTOR).orElse(false)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 멘토 ID입니다.");
        }
        if (!memberRepository.findById(menteeId).map(m -> m.getRole() == Role.MENTEE).orElse(false)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 멘티 ID입니다.");
        }
        if (!relationRepository.findByMentorIdAndActivateYn(mentorId, "Y").stream()
                .anyMatch(r -> r.getMenteeId().equals(menteeId))) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "멘토와 멘티 간의 관계가 존재하지 않습니다.");
        }
    }
}
