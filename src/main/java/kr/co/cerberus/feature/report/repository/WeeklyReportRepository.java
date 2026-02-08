package kr.co.cerberus.feature.report.repository;

import kr.co.cerberus.feature.report.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    List<WeeklyReport> findByMentorIdAndMenteeIdAndReportDateBetweenAndActivateYn(Long mentorId, Long menteeId, LocalDate startDate, LocalDate endDate, String activateYn);
    List<WeeklyReport> findByMenteeIdAndActivateYn(Long menteeId, String activateYn);
    //  멘티의 특정 주차 주간리포트 아이디 조회
    Long findWeeklyReportIdByMenteeIdAndReportDateBetweenAndActivateYn(Long menteeId, LocalDate startDate, LocalDate endDate, String activateYn);
    WeeklyReport findByMenteeIdAndReportDateBetween(Long menteeId, LocalDate startDate, LocalDate endDate);
}
