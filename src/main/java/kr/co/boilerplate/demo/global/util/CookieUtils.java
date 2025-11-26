package kr.co.boilerplate.demo.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
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
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    // 쿠키 삭제하기
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    // 쿠키에 인증 관련 정보를 직렬화하여 저장하기 위한 메소드
    public static String serialize(Object object) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos)) {

			oos.writeObject(object);
			return Base64.getUrlEncoder().encodeToString(baos.toByteArray());

		} catch (IOException e) {
			throw new IllegalArgumentException("객체 직렬화 중 에러 발생", e);
		}
    }

    // 쿠키에서 정보를 역직렬화하여 읽기 위한 메소드
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
		if (cookie == null || cookie.getValue() == null) {
			return null;
		}

		byte[] data = Base64.getUrlDecoder().decode(cookie.getValue());

		try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
				ObjectInputStream ois = new ObjectInputStream(bais)) {

			Object object = ois.readObject();
			return cls.cast(object);

		} catch (ClassNotFoundException | IOException e) {
			throw new IllegalArgumentException("쿠키 역직렬화 중 에러 발생", e);
		}
    }
}