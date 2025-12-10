package kr.co.boilerplate.demo.feature.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.co.boilerplate.demo.feature.auth.dto.AuthDtos.*;
import kr.co.boilerplate.demo.feature.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "상태 확인 API", description = "서버 상태를 확인합니다.")
	@GetMapping("/health-check")
	public ResponseEntity<Void> healthCheck(){
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "일반 로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@Operation(summary = "회원가입", description = "일반 이메일 회원가입을 진행합니다.")
	@PostMapping("/signup")
	public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.ok(authService.signup(request));
	}
}