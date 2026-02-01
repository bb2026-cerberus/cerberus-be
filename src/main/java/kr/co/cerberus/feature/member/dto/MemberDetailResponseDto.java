package kr.co.cerberus.feature.member.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberDetailResponseDto {
    private Long id;
    private String nickName;
    private String email;
    private String password;
    private String role;
    private LocalDateTime createdTime;
}