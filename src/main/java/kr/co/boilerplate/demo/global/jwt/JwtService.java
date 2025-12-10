package kr.co.boilerplate.demo.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.boilerplate.demo.feature.member.Repository.MemberRepository;
import kr.co.boilerplate.demo.global.util.CookieUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Getter
@Slf4j
public class JwtService {
	
	@Value("${jwt.secretKey}")
	private String inputSecretKey;
	
	@Value("${jwt.access.expiration}")
	private Long accessTokenExpirationPeriod;
	
	@Value("${jwt.refresh.expiration}")
	private Long refreshTokenExpirationPeriod;
	
	@Value("${jwt.access.header}")
	private String accessHeader;
	
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
	
	private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
	private static final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
	private static final String EMAIL_CLAIM = "email";
	private static final String ROLE_CLAIM = "role";
	private static final String BEARER = "Bearer ";
	
	private final MemberRepository memberRepository;
	private SecretKey secretKey;
	
	@PostConstruct
	public void init() {
		this.secretKey = Keys.hmacShaKeyFor(inputSecretKey.getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * AccessToken 생성
	 */
	public String createAccessToken(String email, String role) {
		Date now = new Date();
		return Jwts.builder()
				.subject(ACCESS_TOKEN_SUBJECT)
				.claim(EMAIL_CLAIM, email)
				.claim(ROLE_CLAIM, role)
				.expiration(new Date(now.getTime() + accessTokenExpirationPeriod))
				.signWith(secretKey)
				.compact();
	}
	
	/**
	 * RefreshToken 생성
	 */
	public String createRefreshToken() {
		Date now = new Date();
		return Jwts.builder()
				.subject(REFRESH_TOKEN_SUBJECT)
				.expiration(new Date(now.getTime() + refreshTokenExpirationPeriod))
				.signWith(secretKey)
				.compact();
	}
	
	/**
	 * AccessToken 헤더 설정
	 */
	public void sendAccessToken(HttpServletResponse response, String accessToken) {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setHeader(accessHeader, accessToken); // AccessToken은 헤더로
		log.info("재발급된 Access Token : {}", accessToken);
	}
	
	/**
	 * 로그인 시: AccessToken(Header) + RefreshToken(Cookie) 설정
	 * (기존 sendAccessAndRefreshToken 대체)
	 */
	public void sendAccessAndRefreshToken(HttpServletResponse response, String accessToken, String refreshToken) {
		response.setStatus(HttpServletResponse.SC_OK);
		
		response.setHeader(accessHeader, accessToken);
		addRefreshTokenCookie(response, refreshToken);
		
		log.info("Access Token(Header), Refresh Token(Cookie) 설정 완료");
	}
	
	/**
	 * 헤더에서 AccessToken 추출
	 */
	public Optional<String> extractAccessToken(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(accessHeader))
				.filter(token -> token.startsWith(BEARER))
				.map(token -> token.replace(BEARER, ""));
	}
	
	/**
	 * 쿠키에서 RefreshToken 추출 (변경됨)
	 * Request의 Cookies 배열에서 refresh_token 이름을 가진 쿠키를 찾습니다.
	 */
	public Optional<String> extractRefreshToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		
		return Arrays.stream(cookies)
				.filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst();
	}
	
	/**
	 * AccessToken에서 Email 추출
	 */
	public Optional<String> extractEmail(String accessToken) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(accessToken)
					.getPayload();
			return Optional.ofNullable(claims.get(EMAIL_CLAIM, String.class));
		} catch (Exception e) {
			log.error("액세스 토큰이 유효하지 않습니다.");
			return Optional.empty();
		}
	}
	
	/**
	 * RefreshToken DB 저장/업데이트
	 */
	@Transactional
	public void updateRefreshToken(String email, String refreshToken) {
		memberRepository.findByEmail(email)
				.ifPresentOrElse(
						member -> member.updateRefreshToken(refreshToken),
						() -> { throw new IllegalArgumentException("일치하는 회원이 없습니다."); }
				);
	}
	
	/**
	 * 토큰 유효성 검사
	 */
	public boolean isTokenValid(String token) {
		try {
			Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.error("만료된 토큰입니다.");
			return false;
		} catch (JwtException | IllegalArgumentException e) {
			log.error("유효하지 않은 토큰입니다.");
			return false;
		}
	}
	
	/**
	 * Refresh Token 쿠키 생성 및 Response에 추가
	 */
	public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
		int maxAge = (int) (refreshTokenExpirationPeriod / 1000);
		
		CookieUtils.addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, maxAge, "/auth/reissue");
	}
	
	/**
	 * Refresh Token 쿠키 삭제
	 */
	public void removeRefreshTokenCookie(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
				.path("/auth/reissue")
				.maxAge(0)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}