package kr.co.cerberus.feature.relation;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(schema = "master", name = "tb_relation")
public class Relation {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rel_seq_generator")
	@SequenceGenerator(
			name = "rel_seq_generator",
			sequenceName = "master.tb_relation_rel_seq_seq",
			allocationSize = 1
	)
	@Column(name = "relation_seq")
	private Long id;

	@Column(name = "mentor_seq")
	private Long mentorSeq;

	@Column(name = "mentee_seq")
	private Long menteeSeq;
}
