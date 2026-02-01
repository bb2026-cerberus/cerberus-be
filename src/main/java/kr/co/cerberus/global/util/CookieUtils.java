package kr.co.cerberus.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.Optional;

public class CookieUtils {

    // 쿠키 가져오기
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    // 쿠키 읽기
    public static Optional<String> readServletCookie(HttpServletRequest request, String name) {
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findAny();
    }

    // 쿠키 추가하기
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path(path)
                .httpOnly(true)
                .maxAge(maxAge);

        // 운영 환경(HTTPS)에서는 Secure=true, SameSite=None
        // 개발 환경(HTTP)에서는 Secure=false, SameSite=Lax
        // TODO: 실제 운영 환경에서는 프로파일이나 request.isSecure() 확인 로직 필요
        // 여기서는 기본적으로 Lax 설정을 사용하되, 필요시 None + Secure 주석 해제 사용
        
        // boolean isSecure = true; // 운영 환경 플래그
        // if (isSecure) {
        //    builder.secure(true).sameSite("None");
        // } else {
            builder.secure(false).sameSite("Lax");
        // }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    // 쿠키 삭제하기
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    ResponseCookie deleteCookie = ResponseCookie.from(name, "")
                            .path("/")
                            .maxAge(0)
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
                }
            }
        }
    }
}
