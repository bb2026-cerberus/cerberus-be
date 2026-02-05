package kr.co.cerberus.feature.todo;

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
@Table(schema = "master", name = "tb_todo")
public class Todo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "todo_seq")
	private Long id;

	@Column(name = "mentee_seq")
	private Long menteeId;

	@Column(name = "goal_seq")
	private Long goalId;

	@Column(name = "todo_date")
	private LocalDate todoDate;

	@Column(name = "todo_name")
	private String todoName;

	@Column(name = "todo_note")
	private String todoNote;

	@Column(name = "todo_file", columnDefinition = "jsonb")
	private String todoFile;

	@Column(name = "todo_subjects", columnDefinition = "bpchar(1)")
	private String todoSubjects;

	@Builder.Default
	@Column(name = "todo_assign_yn", columnDefinition = "bpchar(1) default 'N'")
	private String todoAssignYn = "N";

	@Builder.Default
	@Column(name = "todo_complete_yn", columnDefinition = "bpchar(1) default 'N'")
	private String todoCompleteYn = "N";

	@Builder.Default
	@Column(name = "todo_temp_yn", columnDefinition = "bpchar(1) default 'N'")
	private String todoTempYn = "N";

	@Column(name = "todo_start_dt")
	private LocalDateTime todoStartDt;

	@Column(name = "todo_end_dt")
	private LocalDateTime todoEndDt;

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

	public void toggleComplete() {
		this.todoCompleteYn = "Y".equals(this.todoCompleteYn) ? "N" : "Y";
	}

	public void markComplete() {
		this.todoCompleteYn = "Y";
	}

	public void updateTodoFile(String todoFile) {
		this.todoFile = todoFile;
	}
}
