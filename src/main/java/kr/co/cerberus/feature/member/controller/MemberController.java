package kr.co.cerberus.feature.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.member.dto.LoginRequestDto;
import kr.co.cerberus.feature.member.dto.LoginResponseDto;
import kr.co.cerberus.feature.member.service.MemberService;
import kr.co.cerberus.feature.member.dto.MemberDetailResponseDto;
import kr.co.cerberus.feature.member.dto.MemberListResponseDto;
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
	
	@Operation(summary = "핑 테스트", description = "핑 테스트 전용 API")
	@GetMapping("/ping")
	public ResponseEntity<?> pint() {
		return ResponseEntity.ok().build();
	}

    @Operation(summary = "로그인", description = "로그인 API")
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(CommonResponse.of(memberService.login(request.name(), request.password())));
    }
    // 회원 삭제
    @Operation(summary = "회원 삭제", description = "회원 ID를 기반으로 회원을 삭제(탈퇴) 처리합니다.")
    @DeleteMapping("/member/{id}/delete")
    public ResponseEntity<CommonResponse<Void>> memberDelete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.ok().build();
    }

    // 회원 상세 정보 조회
    @Operation(summary = "회원 상세 조회", description = "회원 ID를 기반으로 회원의 상세 정보를 조회합니다.")
    @GetMapping("/member/{id}/detail")
    public ResponseEntity<CommonResponse<MemberDetailResponseDto>> memberDetail(@PathVariable Long id) {
        MemberDetailResponseDto memberDetail = memberService.findMemberDetail(id);
        return ResponseEntity.ok(CommonResponse.of(memberDetail));
    }

    // 회원 목록 조회
    @Operation(summary = "회원 목록 조회 (관리자)", description = "전체 회원 목록을 조회합니다. 관리자 권한이 필요합니다.")
    @GetMapping("admin/member/list")
    public ResponseEntity<CommonResponse<MemberListResponseDto>> memberList() {
        MemberListResponseDto memberListResponse = memberService.findAll();
        return ResponseEntity.ok(CommonResponse.of(memberListResponse));
    }
}