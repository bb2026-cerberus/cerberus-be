package kr.co.boilerplate.demo.feature.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

@Slf4j
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    /**
     * 로그인 실패 시 작동하게 되는 Handler
     * Error Status: 400
     * Error Message: "로그인 실패! 이메일이나 비밀번호를 확인해주세요."
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(ErrorResponseDto.makeMessage(HttpStatus.BAD_REQUEST, "로그인 실패! 이메일이나 비밀번호를 확인해주세요.").toString());
        log.info("로그인에 실패했습니다. 메시지 : {}", exception.getMessage());
    }
}