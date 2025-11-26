package kr.co.boilerplate.demo.feature.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.boilerplate.demo.feature.member.Member;
import kr.co.boilerplate.demo.feature.member.Repository.MemberRepository;
import kr.co.boilerplate.demo.feature.member.Role;
import kr.co.boilerplate.demo.global.util.CookieUtils;
import kr.co.boilerplate.demo.global.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

import static kr.co.boilerplate.demo.feature.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final MemberRepository memberRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("OAuth2 Login 성공!");
		CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

		String accessToken = jwtService.createAccessToken(oAuth2User.getEmail(), oAuth2User.getRole().name());
		String refreshToken = jwtService.createRefreshToken();

		if(oAuth2User.getRole() == Role.GUEST) {
			Member findMember = memberRepository.findByEmail(oAuth2User.getEmail())
							.orElseThrow(() -> new IllegalArgumentException("이메일에 해당하는 유저가 없습니다."));
			findMember.authorizeUser();

		} else {
			jwtService.updateRefreshToken(oAuth2User.getEmail(), refreshToken);
		}

		addCookie(response, "refresh_token", refreshToken, 7 * 24 * 60 * 60); // 7일

		String targetUrl = determineTargetUrl(request, response, authentication);
		targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
				.queryParam("accessToken", accessToken)
				.build().toUriString();

		clearAuthenticationAttributes(request, response);
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUrl = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        String targetUrl = redirectUrl.orElse(getDefaultTargetUrl());

        return UriComponentsBuilder.fromUriString(targetUrl).toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

	private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/auth/reissue");
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}
}
