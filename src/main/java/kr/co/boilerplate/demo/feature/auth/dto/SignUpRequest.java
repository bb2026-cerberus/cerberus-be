package kr.co.boilerplate.demo.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignUpRequest(
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식이 아닙니다.")
		String email,
		@NotBlank(message = "비밀번호는 필수입니다.")
		@Pattern(regexp = "^(?:(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)|(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*?&])|(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])|(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])).{8,20}$",
				message = "비밀번호는 8~20자 영문 대/소문자, 숫자, 특수문자 중 3가지 이상을 조합하여 사용해야 합니다.")
		String password
) {

}
