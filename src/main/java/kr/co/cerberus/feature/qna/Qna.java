package kr.co.cerberus.feature.qna;

import jakarta.persistence.*;
import kr.co.cerberus.global.entity.BaseEntity;
import lombok.Builder;
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
@Table(schema = "master", name = "tb_qna")
public class Qna extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qna_seq")
    private Long id;

    @Column(name = "mentee_seq", nullable = false)
    private Long menteeId;

    @Column(name = "mentor_seq", nullable = false)
    private Long mentorId;

    @Column(name = "qna_date", nullable = false)
    private LocalDate qnaDate;

    @Column(name = "question_content", columnDefinition = "TEXT", nullable = false)
    private String questionContent;

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "qna_file", columnDefinition = "jsonb")
    private String qnaFile;

    @Builder.Default
    @Column(name = "qna_complete_yn", columnDefinition = "bpchar(1) default 'N'", nullable = false)
    private String qnaCompleteYn = "N";

    public void updateQuestion(String questionContent, String qnaFile) {
        this.questionContent = questionContent;
        this.qnaFile = qnaFile;
    }

    public void updateAnswer(String answerContent) {
        this.answerContent = answerContent;
        this.qnaCompleteYn = "Y"; // 답변 시 완료 상태로 변경
    }
}
