package kr.co.cerberus.feature.goal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_goal")
public class Goal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "goal_seq")
	private Long id;

	@Column(name = "goal_subjects", columnDefinition = "bpchar(1)")
	private String goalSubjects;

	@Column(name = "goal_name")
	private String goalName;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "goal_file", columnDefinition = "jsonb")
	private String goalFile;

	@Column(name = "create_dt")
	private LocalDateTime createDt;

	@Column(name = "update_dt")
	private LocalDateTime updateDt;

	@Builder.Default
	@Column(name = "delete_yn", columnDefinition = "bpchar(1) default 'N'")
	private String deleteYn = "N";

	@Builder.Default
	@Column(name = "activate_yn", columnDefinition = "bpchar(1) default 'Y'")
	private String activateYn = "Y";

	@PrePersist
	protected void onCreate() {
		this.createDt = LocalDateTime.now();
		this.updateDt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updateDt = LocalDateTime.now();
	}
}
