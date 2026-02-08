package kr.co.cerberus.feature.member;

import jakarta.persistence.*;
import kr.co.cerberus.global.entity.BaseEntity;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(schema = "master", name = "tb_member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mem_seq_generator")
    @SequenceGenerator(
            name = "mem_seq_generator",
            sequenceName = "master.tb_member_mem_seq_seq",
            allocationSize = 1
    )
    @Column(name = "mem_seq")
    private Long id;

    @Column(name ="mem_id")
    private String memId;

    @Column(name = "mem_name")
    private String memName;

    @Column(name = "mem_passwd")
    private String memPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "mem_role", length = 30)
    private Role role;
}