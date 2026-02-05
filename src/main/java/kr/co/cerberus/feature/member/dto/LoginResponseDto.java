package kr.co.cerberus.feature.member.dto;

import kr.co.cerberus.feature.member.Member;
import lombok.Builder;

@Builder
public record LoginResponseDto (
        Long id,
        String role
) {
    public static LoginResponseDto from(Member member) {
        return LoginResponseDto.builder()
                .id(member.getId())
                .role(member.getRole().name())
                .build();
    }
}
