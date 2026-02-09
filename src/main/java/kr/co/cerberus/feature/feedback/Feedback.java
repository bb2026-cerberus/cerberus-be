package kr.co.cerberus.feature.feedback;

import jakarta.persistence.*;
import kr.co.cerberus.global.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Entity
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_feedback")
public class Feedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_seq")
    private Long id;

    @Column(name = "todo_seq", nullable = false)
    private Long todoId;

    @Column(name = "mentee_seq", nullable = false)
    private Long menteeId;

    @Column(name = "mentor_seq", nullable = false)
    private Long mentorId;

    @Column(name = "feed_date")
    private LocalDate feedDate;

    @Column(name = "feed_summary", columnDefinition = "TEXT")
    private String summary; // 중요 요약

    @Column(name = "feed_content", columnDefinition = "TEXT")
    private String content; // 피드백 내용

    @Builder.Default
    @Column(name = "feed_draft_yn", columnDefinition = "bpchar(1) default 'Y'", nullable = false)
    private String feedDraftYn = "Y";

    @Builder.Default
    @Column(name = "feed_complete_yn", columnDefinition = "bpchar(1) default 'N'", nullable = false)
    private String feedCompleteYn = "N";

    // 피드백 내용 업데이트 메서드
    public void updateFeedback(String summary, String content, String draftYn, String completeYn) {
        this.summary = summary;
        this.content = content;
        this.feedDraftYn = draftYn;
        this.feedCompleteYn = completeYn;
        this.feedDate = LocalDate.now();
    }
}