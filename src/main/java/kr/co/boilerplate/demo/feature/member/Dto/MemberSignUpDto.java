package kr.co.boilerplate.demo.feature.member.Dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberSignUpDto {
    private String email;
    private String password;
}
