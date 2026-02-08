package kr.co.cerberus.feature.todo;

import jakarta.persistence.*;
import kr.co.cerberus.global.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_todo")
public class Todo extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "todo_seq")
	private Long id;

	@Column(name = "mentee_seq", nullable = false)
	private Long menteeId;

	@Column(name = "goal_seq")
	private Long goalId;

	@Column(name = "solution_seq")
	private Long solutionId;

	@Column(name = "todo_date", nullable = false)
	private LocalDate todoDate;

	@Column(name = "todo_name", nullable = false)
	private String todoName;

	@Column(name = "todo_note")
	private String todoNote;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "todo_file", columnDefinition = "jsonb")
	private String todoFile;

	@Column(name = "todo_subjects", columnDefinition = "varchar(10)")
	private String todoSubjects;

	@Builder.Default
	@Column(name = "todo_assign_yn", columnDefinition = "bpchar(1) default 'N'", nullable = false)
	private String todoAssignYn = "N";

	@Builder.Default
	@Column(name = "todo_complete_yn", columnDefinition = "bpchar(1) default 'N'", nullable = false)
	private String todoCompleteYn = "N";

	@Builder.Default
	@Column(name = "todo_draft_complete_yn", columnDefinition = "bpchar(1) default 'N'", nullable = false)
	private String todoDraftCompleteYn = "N";

	@Column(name = "todo_start_dt")
	private LocalDateTime todoStartDt;

	@Column(name = "todo_end_dt")
	private LocalDateTime todoEndDt;

	public void toggleComplete() {
		this.todoCompleteYn = "Y".equals(this.todoCompleteYn) ? "N" : "Y";
	}

	public void markComplete() {
		this.todoCompleteYn = "Y";
		this.todoDraftCompleteYn = "Y";
	}

	public void updateTodoFile(String todoFile) {
		this.todoFile = todoFile;
	}

	public void markIncomplete() {
		this.todoCompleteYn = "N";
	}

	// 임시저장 상태로 설정
	public void markAsDraft() {
		this.todoDraftCompleteYn = "N";
		this.todoAssignYn = "N";
	}

	// 과제 할당 상태로 설정
	public void assign() {
		this.todoAssignYn = "Y";
		this.todoDraftCompleteYn = "Y";
	}
}
