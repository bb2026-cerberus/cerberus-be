package kr.co.boilerplate.demo.feature.auth.service;

import kr.co.boilerplate.demo.feature.auth.dto.AuthDtos.*;
import kr.co.boilerplate.demo.feature.member.Member;
import kr.co.boilerplate.demo.feature.member.Repository.MemberRepository;
import kr.co.boilerplate.demo.feature.member.Role;
import kr.co.boilerplate.demo.global.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public Long signup(SignupRequest request) {
        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(Role.USER)
                .socialType("LOCAL")
                .build();

        return memberRepository.save(member).getId();
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtService.createAccessToken(member.getEmail(), member.getRole().name());
        String refreshToken = jwtService.createRefreshToken();

        member.updateRefreshToken(refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }
}