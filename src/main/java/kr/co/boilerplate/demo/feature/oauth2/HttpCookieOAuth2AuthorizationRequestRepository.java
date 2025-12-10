package kr.co.boilerplate.demo.feature.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.boilerplate.demo.global.util.CookieUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    private final ObjectMapper objectMapper;

    // 쿠키로부터 요청 정보를 가져오기
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> deserialize(cookie.getValue()))
                .orElse(null);
    }

    // 인증 정보를 쿠키에 저장
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        String value = serialize(authorizationRequest);
        CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, value, COOKIE_EXPIRE_SECONDS, "/");
        
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.isNotBlank(redirectUriAfterLogin)) {
            CookieUtils.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS, "/");
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    // 쿠키에 담긴 정보 지우기
    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    // OAuth2AuthorizationRequest -> JSON String (Base64 Encoded)
    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("authorizationUri", authorizationRequest.getAuthorizationUri());
            map.put("authorizationGrantType", authorizationRequest.getGrantType().getValue());
            map.put("responseType", authorizationRequest.getResponseType().getValue());
            map.put("clientId", authorizationRequest.getClientId());
            map.put("redirectUri", authorizationRequest.getRedirectUri());
            map.put("scopes", authorizationRequest.getScopes());
            map.put("state", authorizationRequest.getState());
            map.put("additionalParameters", authorizationRequest.getAdditionalParameters());
            map.put("attributes", authorizationRequest.getAttributes());
            // Note: attributes and additionalParameters must be serializable. 
            // Usually they are Strings or simple types.

            String json = objectMapper.writeValueAsString(map);
            return Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("OAuth2AuthorizationRequest 직렬화 실패", e);
        }
    }

    // JSON String (Base64 Encoded) -> OAuth2AuthorizationRequest
    @SuppressWarnings("unchecked")
    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] data = Base64.getUrlDecoder().decode(value);
            String json = new String(data, StandardCharsets.UTF_8);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);

            String authorizationUri = (String) map.get("authorizationUri");
            String clientId = (String) map.get("clientId");
            
            if (authorizationUri == null || clientId == null) {
                return null;
            }

            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri((String) map.get("redirectUri"))
                    .state((String) map.get("state"));
            
            if (map.get("scopes") != null) {
                builder.scopes((java.util.Set<String>) map.get("scopes"));
            }
            
            if (map.get("additionalParameters") != null) {
                builder.additionalParameters((Map<String, Object>) map.get("additionalParameters"));
            }
            
            if (map.get("attributes") != null) {
                builder.attributes((Map<String, Object>) map.get("attributes"));
            }

            return builder.build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("OAuth2AuthorizationRequest 역직렬화 실패", e);
        }
    }
}
