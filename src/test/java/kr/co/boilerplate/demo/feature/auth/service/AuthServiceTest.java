package kr.co.boilerplate.demo.feature.auth.service;

import kr.co.boilerplate.demo.feature.auth.dto.AuthDtos.*;
import kr.co.boilerplate.demo.feature.member.Member;
import kr.co.boilerplate.demo.feature.member.Repository.MemberRepository;
import kr.co.boilerplate.demo.feature.member.Role;
import kr.co.boilerplate.demo.global.jwt.JwtService;
import kr.co.boilerplate.demo.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        SignupRequest request = new SignupRequest("test@test.com", "password");
        given(memberRepository.findByEmail(request.email())).willReturn(Optional.empty());
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        
        Member savedMember = Member.builder()
                .id(1L)
                .email(request.email())
                .password("encodedPassword")
                .role(Role.USER)
                .build();
        
        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        // when
        Long memberId = authService.signup(request);

        // then
        assertThat(memberId).isEqualTo(1L);
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("중복 이메일 회원가입 실패")
    void signup_fail_duplicate_email() {
        // given
        SignupRequest request = new SignupRequest("test@test.com", "password");
        given(memberRepository.findByEmail(request.email())).willReturn(Optional.of(Member.builder().build()));

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("test@test.com", "password");
        Member member = Member.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        given(memberRepository.findByEmail(request.email())).willReturn(Optional.of(member));
        given(passwordEncoder.matches(request.password(), member.getPassword())).willReturn(true);
        given(jwtService.createAccessToken(member.getEmail(), member.getRole().name())).willReturn("accessToken");
        given(jwtService.createRefreshToken()).willReturn("refreshToken");

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
        assertThat(member.getRefreshToken()).isEqualTo("refreshToken");
    }
}
