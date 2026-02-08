package kr.co.cerberus.feature.report.service;

import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.report.WeeklyReport;
import kr.co.cerberus.feature.report.dto.WeeklyReportCreateRequestDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportResponseDto;
import kr.co.cerberus.feature.report.dto.WeeklyReportUpdateRequestDto;
import kr.co.cerberus.feature.report.repository.WeeklyReportRepository;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final MemberRepository memberRepository; // 멘티/멘토 유효성 검증용
    private final RelationRepository relationRepository; // 멘토-멘티 관계 검증용

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
    public List<WeeklyReportResponseDto> getWeeklyReportsByMentee(Long menteeId) {
        List<WeeklyReport> reports = weeklyReportRepository.findByMenteeIdAndActivateYn(menteeId, "Y");
        return reports.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
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
