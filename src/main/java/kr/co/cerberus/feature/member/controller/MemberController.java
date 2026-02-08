package kr.co.cerberus.feature.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.member.dto.LoginRequestDto;
import kr.co.cerberus.feature.member.dto.LoginResponseDto;
import kr.co.cerberus.feature.member.service.MemberService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Tag(name = "Member", description = "회원 관리 관련 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "로그인", description = "로그인 API")
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(CommonResponse.of(memberService.login(request.id(), request.password())));
    }
}