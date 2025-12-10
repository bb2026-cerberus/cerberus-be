package kr.co.boilerplate.demo.feature.member;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

	// Todo: 리프레시 토큰 Redis에서 관리하도록 수정할건지 고민 필요
    private String refreshToken; // 리프레시 토큰

    private LocalDateTime createdTime;

    @Builder.Default
	@Column(name = "delete_yn", columnDefinition = "char(1) default 'N'")
    private String deleteYn = "N";

    public void deleteMember() {
        this.deleteYn = "Y";
    }

    public void passwordEncode(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }

    public void authorizeUser() {
        this.role = Role.USER;
    }

    public void updateRefreshToken(String updateRefreshToken) {
        this.refreshToken = updateRefreshToken;
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
