package kr.co.cerberus.feature.member.dto;

import lombok.Data;

@Data
public class MemberUpdateRequestDto {
    private String email;
    private String imageUrl;
}
