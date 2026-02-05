package kr.co.cerberus.feature.planner;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_planner")
public class Planner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "plan_seq")
	private Long id;

	@Column(name = "mentee_seq")
	private Long menteeId;

	@Column(name = "plan_date")
	private LocalDate planDate;

	@Column(name = "plan_file", columnDefinition = "jsonb")
	private String planFile;

	@Column(name = "create_dt")
	private LocalDateTime createDt;

	@Column(name = "update_dt")
	private LocalDateTime updateDt;

	@Builder.Default
	@Column(name = "delete_yn", columnDefinition = "bpchar(1) default 'N'")
	private String deleteYn = "N";

	@Builder.Default
	@Column(name = "activate_tn", columnDefinition = "bpchar(1) default 'Y'")
	private String activateYn = "Y";

	@Column(name = "plan_feedback", columnDefinition = "text")
	private String planFeedback;

	@PrePersist
	protected void onCreate() {
		this.createDt = LocalDateTime.now();
		this.updateDt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updateDt = LocalDateTime.now();
	}

	public void updatePlanFile(String planFile) {
		this.planFile = planFile;
	}

	public void updatePlanFeedback(String planFeedback) {
		this.planFeedback = planFeedback;
	}
}
