package kr.co.cerberus.feature.solution;

import jakarta.persistence.*;
import kr.co.cerberus.global.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_solution")
public class Solution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solution_seq")
    private Long id;

    @Column(name = "mentor_seq", nullable = false)
    private Long mentorId; // 솔루션 등록 멘토 ID

    @Column(name = "title", nullable = false)
    private String title; // 솔루션 제목

    @Column(name = "description")
    private String description; // 솔루션 설명

    @Column(name = "subject", columnDefinition = "varchar(10)")
    private String subject; // 과목

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "solution_file", columnDefinition = "jsonb")
    private String solutionFile; // JSONB, FileInfo 리스트 (학습지 파일 등)

    public void updateSolution(String title, String description, String subject, String solutionFile) {
        this.title = title;
        this.description = description;
        this.subject = subject;
        this.solutionFile = solutionFile;
    }
}
