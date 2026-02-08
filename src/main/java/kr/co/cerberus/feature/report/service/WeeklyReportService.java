package kr.co.cerberus.feature.report.service;

import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.report.WeeklyReport;
import kr.co.cerberus.feature.report.dto.WeeklyReportCreateRequestDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportResponseDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportUpdateRequestDto;
import kr.co.cerberus.feature.report.repository.WeeklyReportRepository;
import kr.co.cerberus.feature.relation.Relation;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.util.JsonbUtils;
import kr.co.cerberus.global.jsonb.FeedbackFileData;
import lombok.RequiredArgsConstructor;
// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final MemberRepository memberRepository;
    private final RelationRepository relationRepository;
    private final FeedbackRepository feedbackRepository;
    // private final ChatClient chatClient;

    // AI 주석 처리에 따라 생성자 수정
    /*
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
    */

    @Transactional
    public WeeklyReportResponseDto createWeeklyReport(Long mentorId, WeeklyReportCreateRequestDto requestDto) {
        if (!Objects.equals(requestDto.mentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 리포트만 생성할 수 있습니다.");
        }
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
                requestDto.improvements(),
                JsonbUtils.toJson(requestDto.reportFiles())
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

    public WeeklyReportResponseDto getWeeklyReportDetail(Long reportId) {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponseDto(report);
    }

    public List<WeeklyReportResponseDto> getMenteesWeeklyReports(Long mentorId, LocalDate mondayDate) {
        List<Long> menteeIds = relationRepository.findByMentorId(mentorId)
                .stream()
                .map(Relation::getMenteeId)
                .collect(Collectors.toList());

        List<WeeklyReportResponseDto> results = new ArrayList<>();
        LocalDate sundayDate = mondayDate.plusDays(6);

        for (Long menteeId : menteeIds) {
            Optional<WeeklyReport> existingReport = weeklyReportRepository.findByMenteeIdAndReportDate(menteeId, mondayDate);
            
            if (existingReport.isPresent()) {
                results.add(mapToResponseDto(existingReport.get()));
            } else {
                results.add(generateEmptyReportDraft(mentorId, menteeId, mondayDate));
            }
        }
        return results;
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
                .reportFiles(List.of())
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
                .reportFiles(Optional.ofNullable(JsonbUtils.fromJson(report.getReportFile(), new TypeReference<List<kr.co.cerberus.global.jsonb.FileInfo>>() {})).orElse(List.of()))
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
        if (!relationRepository.findByMentorId(mentorId).stream()
                .anyMatch(r -> r.getMenteeId().equals(menteeId))) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "멘토와 멘티 간의 관계가 존재하지 않습니다.");
        }
    }
}
