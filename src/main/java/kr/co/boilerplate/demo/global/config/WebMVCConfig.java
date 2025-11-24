package kr.co.boilerplate.demo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 설정 (CORS 설정 포함)
 *
 * <p>
 * <strong>CORS(Cross-Origin Resource Sharing) 설정</strong><br>
 * 브라우저의 SOP(Same-Origin Policy, 동일 출처 정책)로 인해 발생하는
 * 다른 도메인 간의 리소스 요청 차단을 해제하기 위한 설정입니다.
 * </p>
 *
 * <b>Todo</b>
 * <ul>
 * <li>프론트엔드 개발 서버와 API 서버의 도메인(포트)이 다르기 때문에 설정이 필요합니다.</li>
 * <li>보안상 필요한 최소한의 도메인과 메서드만 허용하는 것을 권장합니다.</li>
 * </ul>
 */
@Configuration
public class WebMVCConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")		// 모든 경로에 대해 CORS 설정을 적용
				.allowedOrigins("http://localhost:3000") // 허용할 프론트엔드 도메인
				.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // 허용할 HTTP 메서드
				.allowedHeaders("*")               // 요청 시 허용할 헤더 (JWT 등 커스텀 헤더 포함)
				.allowCredentials(true)            // 자격 증명 허용 (쿠키, 세션, Authorization 헤더 등)
				.maxAge(3600);                     // Preflight 요청(OPTIONS)의 캐시 시간 (초 단위, 1시간)
    }
}