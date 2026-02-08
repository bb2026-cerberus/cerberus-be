package kr.co.cerberus.feature.weakness;

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
@Table(schema = "master", name = "tb_weakness_solution")
public class WeaknessSolution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weakness_seq")
    private Long id;

    @Column(name = "mentee_seq", nullable = false)
    private Long menteeId;

    @Column(name = "mentor_seq", nullable = false)
    private Long mentorId;

    @Column(name = "subject", columnDefinition = "varchar(10)", nullable = false)
    private String subject; // 관련 과목

    @Column(name = "weakness_description", columnDefinition = "TEXT", nullable = false)
    private String weaknessDescription; // 약점 설명

    @Column(name = "solution_content", columnDefinition = "TEXT", nullable = false)
    private String solutionContent; // 솔루션 내용/보완점

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "solution_file", columnDefinition = "jsonb")
    private String solutionFile; // JSONB, FileInfo 리스트 (첨부 자료)

    public void updateWeaknessSolution(String subject, String weaknessDescription, String solutionContent, String solutionFile) {
        this.subject = subject;
        this.weaknessDescription = weaknessDescription;
        this.solutionContent = solutionContent;
        this.solutionFile = solutionFile;
    }
}
