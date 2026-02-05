package kr.co.cerberus.feature.feedback;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_feedback")
public class Feedback {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "feed_seq")
	private Long id;

	@Column(name = "todo_seq")
	private Long todoId;

	@Column(name = "feed_file", columnDefinition = "jsonb")
	private String feedFile;

	@Column(name = "feed_date")
	private LocalDate feedDate;

	@Builder.Default
	@Column(name = "delete_yn", columnDefinition = "bpchar(1) default 'N'")
	private String deleteYn = "N";

	@Builder.Default
	@Column(name = "activate_yn", columnDefinition = "bpchar(1) default 'Y'")
	private String activateYn = "Y";
}
