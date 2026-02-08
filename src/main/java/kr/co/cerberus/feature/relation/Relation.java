package kr.co.cerberus.feature.relation;

import jakarta.persistence.*;
import kr.co.cerberus.global.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_relation")
public class Relation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relation_seq")
    private Long id;

    @Column(name = "mentor_seq", nullable = false)
    private Long mentorId;

    @Column(name = "mentee_seq", nullable = false)
    private Long menteeId;
}