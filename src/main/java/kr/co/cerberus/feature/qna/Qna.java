package kr.co.cerberus.feature.qna;

import jakarta.persistence.*;
import kr.co.cerberus.feature.qna.domain.QnaStatus;
import kr.co.cerberus.global.entity.BaseEntity;
import lombok.Builder;
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

    @Column(name = "related_entity_id")
    private Long relatedEntityId; // 관련 Entity ID (예: todoId, solutionId 등)

    @Column(name = "related_entity_type", length = 20)
    private String relatedEntityType; // 관련 Entity 타입 (예: "TODO", "SOLUTION" 등)

    @Column(name = "title", nullable = false)
    private String title; // Q&A 제목

    @Column(name = "question_content", columnDefinition = "TEXT", nullable = false)
    private String questionContent; // 질문 내용

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent; // 답변 내용

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "qna_file", columnDefinition = "jsonb")
    private String qnaFile; // JSONB, FileInfo 리스트 (사진 첨부용)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", columnDefinition = "varchar(20) default 'PENDING'", nullable = false)
    private QnaStatus status = QnaStatus.PENDING;

    public void updateQuestion(String title, String questionContent, String qnaFile, Long relatedEntityId, String relatedEntityType) {
        this.title = title;
        this.questionContent = questionContent;
        this.qnaFile = qnaFile;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
    }

    public void updateAnswer(String answerContent) {
        this.answerContent = answerContent;
        this.status = QnaStatus.ANSWERED; // 답변 시 상태 변경
    }

    public void updateStatus(QnaStatus newStatus) {
        this.status = newStatus;
    }
}
