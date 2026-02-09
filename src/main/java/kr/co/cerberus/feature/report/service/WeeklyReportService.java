package kr.co.cerberus.feature.report.service;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.report.WeeklyReport;
import kr.co.cerberus.feature.report.dto.WeeklyReportCreateRequestDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportResponseDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportUpdateRequestDto;
import kr.co.cerberus.feature.report.repository.WeeklyReportRepository;
import kr.co.cerberus.feature.relation.Relation;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.util.JsonbUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final ChatClient chatClient;

    public WeeklyReportService(
            WeeklyReportRepository weeklyReportRepository,
            MemberRepository memberRepository,
            RelationRepository relationRepository,
            FeedbackRepository feedbackRepository,
            ChatClient.Builder chatClientBuilder) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.memberRepository = memberRepository;
        this.relationRepository = relationRepository;
        this.feedbackRepository = feedbackRepository;
        this.chatClient = chatClientBuilder.build();
    }

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

    public List<WeeklyReportResponseDto> getWeeklyReportsByMentee(Long menteeId) {
        List<WeeklyReport> reports = weeklyReportRepository.findByMenteeId(menteeId);
        return reports.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
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
