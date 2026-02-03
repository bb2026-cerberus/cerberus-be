package kr.co.cerberus.feature.member;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_member")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nickName;
    private String email;
    private String password;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String socialType;

    private String socialId;

    private LocalDateTime createdTime;

    @Builder.Default
	@Column(name = "delete_yn", columnDefinition = "char(1) default 'N'")
    private String deleteYn = "N";

    public void deleteMember() {
        this.deleteYn = "Y";
    }

    public boolean isSocial() {
        return this.socialId != null;
    }

    public Member update(String nickName, String imageUrl) {
		this.nickName = nickName;
		this.imageUrl = imageUrl;
		return this;
    }
}
