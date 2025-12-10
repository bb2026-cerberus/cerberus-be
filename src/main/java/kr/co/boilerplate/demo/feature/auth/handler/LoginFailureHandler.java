package kr.co.boilerplate.demo.feature.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.boilerplate.demo.feature.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import kr.co.boilerplate.demo.global.error.ErrorCode;
import kr.co.boilerplate.demo.global.error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {
	
	private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
	
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
	    
	    log.info("로그인 실패 발생 - IP: {}, Message: {}", request.getRemoteAddr(), exception.getMessage());
	    
		if (exception instanceof InternalAuthenticationServiceException) {
		 log.error("로그인 처리 중 내부 시스템 오류 발생", exception);
		}
		
	    authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
		
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(ErrorResponse.makeMessage(ErrorCode.LOCAL_LOGIN_FAIL));
    }
}