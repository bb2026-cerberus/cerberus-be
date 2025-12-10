package kr.co.boilerplate.demo.feature.member.Dto;

import lombok.Data;

@Data
public class MemberUpdateRequestDto {
    private String email;
    private String imageUrl;
}
