package kr.co.cerberus.feature.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    @Schema(description = "사용자 이름", example = "mentor01")
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    String id,

    @Schema(description = "비밀번호", example = "1234")
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    String password
) {}
