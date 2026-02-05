package kr.co.cerberus.feature.relation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_relation")
public class Relation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "relation_seq")
	private Long id;

	@Column(name = "mentor_seq")
	private Long mentorId;

	@Column(name = "mentee_seq")
	private Long menteeId;
}
