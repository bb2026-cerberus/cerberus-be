package kr.co.cerberus.feature.report.service;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.report.WeeklyReport;
import kr.co.cerberus.feature.report.dto.WeeklyMenteeReportResponseDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportCreateRequestDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportResponseDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportUpdateRequestDto;
import kr.co.cerberus.feature.report.repository.WeeklyReportRepository;
import kr.co.cerberus.feature.relation.Relation;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final MemberRepository memberRepository;
    private final RelationRepository relationRepository;
    private final FeedbackRepository feedbackRepository;
	private final TodoRepository todoRepository;
    private final ChatClient chatClient;

    public WeeklyReportService(
            WeeklyReportRepository weeklyReportRepository,
            MemberRepository memberRepository,
            RelationRepository relationRepository,
            FeedbackRepository feedbackRepository, TodoRepository todoRepository,
            ChatClient.Builder chatClientBuilder) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.memberRepository = memberRepository;
        this.relationRepository = relationRepository;
        this.feedbackRepository = feedbackRepository;
	    this.todoRepository = todoRepository;
	    this.chatClient = chatClientBuilder.build();
    }
	
    // 주간 리포트 생성
    @Transactional
    public WeeklyReportResponseDto createWeeklyReport(Long mentorId, WeeklyReportCreateRequestDto requestDto) {
        if (!Objects.equals(requestDto.mentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 리포트만 생성할 수 있습니다.");
        }
        validateMentorMenteeRelation(mentorId, requestDto.menteeId());

        // 이미 AI 초안이 생성되어 있을 수 있으므로 체크
        WeeklyReport report = weeklyReportRepository.findByMenteeIdAndReportDate(requestDto.menteeId(), requestDto.reportDate())
                .map(existing -> {
                    existing.updateReport(
                            requestDto.summary(),
                            requestDto.overallEvaluation(),
                            requestDto.strengths(),
                            requestDto.improvements()
                    );
                    return existing;
                })
                .orElseGet(() -> WeeklyReport.builder()
                        .menteeId(requestDto.menteeId())
                        .mentorId(mentorId)
                        .reportDate(requestDto.reportDate())
                        .summary(requestDto.summary())
                        .overallEvaluation(requestDto.overallEvaluation())
                        .strengths(requestDto.strengths())
                        .improvements(requestDto.improvements())
                        .build());
        
        WeeklyReport savedReport = weeklyReportRepository.save(report);
        return mapToResponseDto(savedReport);
    }

    @Transactional
    public WeeklyReportResponseDto updateWeeklyReport(Long mentorId, WeeklyReportUpdateRequestDto requestDto) {
        WeeklyReport report = weeklyReportRepository.findById(requestDto.reportId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!Objects.equals(report.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 리포트를 수정할 권한이 없습니다.");
        }

        report.updateReport(
                requestDto.summary(),
                requestDto.overallEvaluation(),
                requestDto.strengths(),
                requestDto.improvements()
        );
        return mapToResponseDto(report);
    }

    @Transactional
    public void deleteWeeklyReport(Long mentorId, Long reportId) {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!Objects.equals(report.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 리포트를 삭제할 권한이 없습니다.");
        }
        report.delete();
    }

    @Transactional
    public WeeklyReportResponseDto getWeeklyReportDetail(Long reportId) {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        
        // 만약 summary가 비어있다면 AI 요약 재시도 가능
        if (report.getSummary() == null || report.getSummary().isBlank() || report.getSummary().equals("작성된 요약이 없습니다.")) {
            return generateAndSaveAiReportDraft(report.getMentorId(), report.getMenteeId(), report.getReportDate());
        }
        
        return mapToResponseDto(report);
    }

    @Transactional
    public List<WeeklyReportResponseDto> getMenteesWeeklyReports(Long mentorId, LocalDate mondayDate) {
        List<Long> menteeIds = relationRepository.findByMentorId(mentorId)
                .stream()
                .map(Relation::getMenteeId)
                .toList();

        List<WeeklyReportResponseDto> results = new ArrayList<>();

        for (Long menteeId : menteeIds) {
            Optional<WeeklyReport> existingReport = weeklyReportRepository.findByMenteeIdAndReportDate(menteeId, mondayDate);
            
            if (existingReport.isPresent()) {
                results.add(mapToResponseDto(existingReport.get()));
            } else {
                results.add(generateAndSaveAiReportDraft(mentorId, menteeId, mondayDate));
            }
        }
        return results;
    }

    // AI를 이용해 초안 생성 및 즉시 DB 저장
    @Transactional
    public WeeklyReportResponseDto generateAndSaveAiReportDraft(Long mentorId, Long menteeId, LocalDate reportDate) {
        LocalDate endDate = reportDate.plusDays(6);
        List<Feedback> feedbacks = feedbackRepository.findByMenteeIdAndFeedDateBetween(menteeId, reportDate, endDate);

        String feedbackContext = feedbacks.stream()
                .map(f -> {
                    return String.format("- [%s] 요약: %s / 상세내용: %s", f.getFeedDate(), f.getSummary(), f.getContent());
                })
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                당신은 멘토링 리포트 작성을 돕는 전문 에이전트입니다.
                다음은 일주일 동안 멘티에게 제공된 피드백 내용들입니다:
                
                %s
                
                이 내용을 바탕으로 주간 리포트 초안을 작성해주세요.
                결과는 반드시 JSON 형식으로 반환해야 하며, 다음 키를 포함해야 합니다:
                - summary: 한 주간의 학습 활동 및 피드백 내용 요약 (3-4문장)
                - strengths: 멘티가 이번 주에 특히 잘했거나 성장한 부분
                
                한국어로 정중하고 격려하는 어조로 작성해주세요.
                """, feedbackContext.isEmpty() ? "해당 주차에 등록된 피드백이 없습니다." : feedbackContext);

        try {
            // Spring AI의 entity() 기능을 사용하여 자동으로 JSON 파싱 및 마크다운 제거 처리
            Map<String, String> aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(new ParameterizedTypeReference<>() {});

            String summary = aiResponse.getOrDefault("summary", "이번 주 활동에 대한 요약 데이터가 부족합니다.");
            String strengths = aiResponse.getOrDefault("strengths", "이번 주 특이사항이 없습니다.");

            WeeklyReport report = weeklyReportRepository.findByMenteeIdAndReportDate(menteeId, reportDate)
                    .orElseGet(() -> WeeklyReport.builder()
                            .mentorId(mentorId)
                            .menteeId(menteeId)
                            .reportDate(reportDate)
                            .build());

            report.updateReport(summary, null, strengths, null);
            WeeklyReport savedReport = weeklyReportRepository.save(report);
            
            return mapToResponseDto(savedReport);

        } catch (Exception e) {
            return generateEmptyReportDraft(mentorId, menteeId, reportDate);
        }
    }

    // AI 대신 빈 초안 반환
    private WeeklyReportResponseDto generateEmptyReportDraft(Long mentorId, Long menteeId, LocalDate reportDate) {
        return WeeklyReportResponseDto.builder()
                .menteeId(menteeId)
                .mentorId(mentorId)
                .reportDate(reportDate)
                .summary("작성된 요약이 없습니다.")
                .overallEvaluation("작성된 총평이 없습니다.")
                .strengths("")
                .improvements("")
                .build();
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
        return WeeklyReportResponseDto.builder()
                .id(report.getId())
                .menteeId(report.getMenteeId())
                .mentorId(report.getMentorId())
                .reportDate(report.getReportDate())
                .summary(report.getSummary())
                .overallEvaluation(report.getOverallEvaluation())
                .strengths(report.getStrengths())
                .improvements(report.getImprovements())
                .createDatetime(report.getCreateDatetime())
                .updateDatetime(report.getUpdateDatetime())
                .build();
    }
    
    private void validateMentorMenteeRelation(Long mentorId, Long menteeId) {
        if (!memberRepository.findById(mentorId).map(m -> m.getRole() == Role.MENTOR).orElse(false)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 멘토 ID입니다.");
        }
        if (!memberRepository.findById(menteeId).map(m -> m.getRole() == Role.MENTEE).orElse(false)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 멘티 ID입니다.");
        }
        if (relationRepository.findByMentorId(mentorId).stream()
                .noneMatch(r -> r.getMenteeId().equals(menteeId))) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "멘토와 멘티 간의 관계가 존재하지 않습니다.");
        }
    }
}
