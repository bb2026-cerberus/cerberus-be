package kr.co.cerberus.feature.report.repository;

import kr.co.cerberus.feature.report.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    List<WeeklyReport> findByMenteeId(Long menteeId);
    Optional<WeeklyReport> findByMenteeIdAndReportDate(Long menteeId, LocalDate reportDate);
	WeeklyReport findByMenteeIdAndReportDateBetween(Long menteeId, LocalDate startDate, LocalDate endDate);
}