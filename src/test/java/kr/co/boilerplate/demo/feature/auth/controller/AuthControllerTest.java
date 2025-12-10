package kr.co.boilerplate.demo.feature.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.boilerplate.demo.feature.auth.dto.AuthDtos;
import kr.co.boilerplate.demo.feature.auth.service.AuthService;
import kr.co.boilerplate.demo.feature.member.Member;
import kr.co.boilerplate.demo.feature.member.Repository.MemberRepository;
import kr.co.boilerplate.demo.feature.member.Role;
import kr.co.boilerplate.demo.global.error.ErrorCode;
import kr.co.boilerplate.demo.global.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        // given
        AuthDtos.SignupRequest request = new AuthDtos.SignupRequest("newuser@test.com", "password1234");

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 실패")
    void signup_fail_duplicate_email() throws Exception {
        // given
        String email = "duplicate@test.com";
        memberRepository.save(Member.builder()
                .email(email)
                .password("password")
                .role(Role.USER)
                .build());

        AuthDtos.SignupRequest request = new AuthDtos.SignupRequest(email, "password1234");

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isBadRequest()) // CustomException 발생 시 400 Bad Request
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // given
        String email = "loginuser@test.com";
        String password = "password1234";
        memberRepository.save(Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .build());

        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest(email, password);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrong_password() throws Exception {
        // given
        String email = "loginuser@test.com";
        String password = "password1234";
        memberRepository.save(Member.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .build());

        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest(email, "wrongpassword");

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isBadRequest()) // CustomException 발생 시 400 Bad Request
                .andDo(print());
    }
}
