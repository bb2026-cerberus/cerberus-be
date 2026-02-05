package kr.co.cerberus.feature.member;

import jakarta.persistence.*;
import kr.co.cerberus.global.entity.BaseEntity;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자 접근 제한
@AllArgsConstructor
@Table(schema = "master", name = "tb_member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mem_seq_generator")
    @SequenceGenerator(
            name = "mem_seq_generator",
            sequenceName = "master.tb_member_mem_seq_seq", // 실제 DB에 생성될 시퀀스명
            allocationSize = 1
    )
    @Column(name = "mem_seq")
    private Long id;

    @Column(name = "mem_name")
    private String name;

    @Column(name = "mem_passwd")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "mem_role", length = 30)
    private Role role;

    @Builder.Default
    @Column(name = "delete_yn", columnDefinition = "bpchar(1)")
    private String deleteYn = "N";

    @Builder.Default
    @Column(name = "activate_yn", columnDefinition = "bpchar(1)")
    private String activateYn = "Y";

    public void deleteMember() {
        this.deleteYn = "Y";
    }
}