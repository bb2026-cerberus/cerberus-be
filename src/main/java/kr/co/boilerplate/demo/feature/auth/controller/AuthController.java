package kr.co.boilerplate.demo.feature.auth.controller;

import kr.co.boilerplate.demo.feature.auth.dto.LoginResponse;
import kr.co.boilerplate.demo.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final LoginService loginService;

	@PostMapping("/login")
	public ResponseEntity<CommonResponse<LoginResponse>> login(@RequestBody Map<String, String> user) {
		return ResponseEntity.ok(CommonResponse.of(loginService.login()));
	}

	@PostMapping("/register")
	public ResponseEntity<CommonResponse<Void>> login(@RequestBody Map<String, String> user) {
		loginService.register();
		return ResponseEntity.ok().build();
	}
}
