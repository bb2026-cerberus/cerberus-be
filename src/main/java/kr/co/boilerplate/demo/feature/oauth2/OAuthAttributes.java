package kr.co.boilerplate.demo.feature.oauth2;

import kr.co.boilerplate.demo.feature.member.Member;
import kr.co.boilerplate.demo.feature.member.Role;
import kr.co.boilerplate.demo.feature.oauth2.dto.*;
import lombok.Builder;

import java.util.Locale;
import java.util.Map;

public record OAuthAttributes(String nameAttributeKey, OAuth2UserInfo oauth2UserInfo) {

	@Builder
	public OAuthAttributes {}

	public static OAuthAttributes of(SocialType socialType, String userNameAttributeName, Map<String, Object> attributes) {
		return switch (socialType) {
			case GOOGLE -> ofGoogle(userNameAttributeName, attributes);
			case KAKAO -> ofKakao(userNameAttributeName, attributes);
			case NAVER -> ofNaver(userNameAttributeName, attributes);
			case GITHUB -> ofGitHub(userNameAttributeName, attributes);
			default -> null;
		};
	}

	private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder().nameAttributeKey(userNameAttributeName)
				.oauth2UserInfo(new KakaoOAuth2UserInfo(attributes)).build();
	}

	public static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder().nameAttributeKey(userNameAttributeName)
				.oauth2UserInfo(new NaverOAuth2UserInfo(attributes)).build();
	}

	public static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder().nameAttributeKey(userNameAttributeName).oauth2UserInfo(new GoogleOAuth2UserInfo(attributes)).build();
	}

	public static OAuthAttributes ofGitHub(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder().nameAttributeKey(userNameAttributeName)
				.oauth2UserInfo(new GitHubOAuth2UserInfo(attributes)).build();
	}

	public Member toEntity(OAuth2UserInfo oauth2UserInfo) {
		return Member.builder().socialType(oauth2UserInfo.getSocialType().toLowerCase(Locale.KOREAN)).socialId(oauth2UserInfo.getId()).email(oauth2UserInfo.getEmail())
				.imageUrl(oauth2UserInfo.getImageUrl()).role(Role.GUEST).build();
	}
}
