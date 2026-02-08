package kr.co.cerberus.feature.report;

import jakarta.persistence.*;
import kr.co.cerberus.global.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Getter
@Entity
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_weekly_report")
public class WeeklyReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_seq")
    private Long id;

    @Column(name = "mentee_seq", nullable = false)
    private Long menteeId;

    @Column(name = "mentor_seq", nullable = false)
    private Long mentorId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate; // 리포트 기준 날짜 (해당 주차의 시작일)

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary; // 주차별 요약

    @Column(name = "overall_evaluation", columnDefinition = "TEXT")
    private String overallEvaluation; // 총평

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths; // 잘한 점

    @Column(name = "improvements", columnDefinition = "TEXT")
    private String improvements; // 보완점

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_file", columnDefinition = "jsonb")
    private String reportFile; // JSONB, FileInfo 리스트 (추가 첨부 자료)

    public void updateReport(String summary, String overallEvaluation, String strengths, String improvements, String reportFile) {
        this.summary = summary;
        this.overallEvaluation = overallEvaluation;
        this.strengths = strengths;
        this.improvements = improvements;
        this.reportFile = reportFile;
    }
}
