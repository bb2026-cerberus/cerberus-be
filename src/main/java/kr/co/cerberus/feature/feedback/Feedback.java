package kr.co.cerberus.feature.feedback;

import jakarta.persistence.*;
import kr.co.cerberus.feature.feedback.domain.FeedbackStatus;
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
@Table(schema = "master", name = "tb_feedback")
public class Feedback extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "feed_seq")
	private Long id;

	@Column(name = "todo_seq", nullable = false)
	private Long todoId;

	// 신규 추가: 멘티 ID
	@Column(name = "mentee_seq", nullable = false)
	private Long menteeId;

	// 신규 추가: 멘토 ID
	@Column(name = "mentor_seq", nullable = false)
	private Long mentorId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "feed_file", columnDefinition = "jsonb")
	private String feedFile;

	@Column(name = "feed_date") // 멘티 API에서 사용하는 필드이므로 유지
	private LocalDate feedDate;

	// 신규 추가: 피드백 상태 (임시저장, 대기중, 완료, 취소됨)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(name = "status", columnDefinition = "varchar(20) default 'DRAFT'", nullable = false)
	private FeedbackStatus status = FeedbackStatus.DRAFT;

	// BaseEntity로 관리되므로 제거
	// @Builder.Default
	// @Column(name = "delete_yn", columnDefinition = "bpchar(1) default 'N'")
	// private String deleteYn = "N";

	// BaseEntity로 관리되므로 제거
	// @Builder.Default
	// @Column(name = "activate_yn", columnDefinition = "bpchar(1) default 'Y'")
	// private String activateYn = "Y";

	// 피드백 파일 내용 업데이트
	public void updateFeedFile(String feedFile) {
		this.feedFile = feedFile;
	}

	// 피드백 상태 업데이트
    public void updateStatus(FeedbackStatus newStatus) {
        this.status = newStatus;
    }

	// 임시저장 상태로 설정
	public void markAsDraft() {
		this.status = FeedbackStatus.DRAFT;
	}

	// 피드백 완료 상태로 설정
	public void markAsCompleted() {
		this.status = FeedbackStatus.COMPLETED;
	}
}
