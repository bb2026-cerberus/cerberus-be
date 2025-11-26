package kr.co.boilerplate.demo.feature.oauth2;

import kr.co.boilerplate.demo.feature.member.Member;
import kr.co.boilerplate.demo.feature.member.Role;
import kr.co.boilerplate.demo.feature.oauth2.dto.GoogleOAuth2UserInfo;
import kr.co.boilerplate.demo.feature.oauth2.dto.KakaoOAuth2UserInfo;
import kr.co.boilerplate.demo.feature.oauth2.dto.NaverOAuth2UserInfo;
import kr.co.boilerplate.demo.feature.oauth2.dto.OAuth2UserInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.Locale;
import java.util.Map;

@Getter
public class OAuthAttributes {

    private String nameAttributeKey;
    private OAuth2UserInfo oauth2UserInfo;

    @Builder
    public OAuthAttributes(String nameAttributeKey, OAuth2UserInfo oauth2UserInfo) {
        this.nameAttributeKey = nameAttributeKey;
        this.oauth2UserInfo = oauth2UserInfo;
    }

	public static OAuthAttributes of(SocialType socialType, String userNameAttributeName, Map<String, Object> attributes) {
		return switch (socialType) {
			case GOOGLE -> ofGoogle(userNameAttributeName, attributes);
			case KAKAO -> ofKakao(userNameAttributeName, attributes);
			case NAVER -> ofNaver(userNameAttributeName, attributes);
		};
	}

	private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder()
				.nameAttributeKey(userNameAttributeName)
				.oauth2UserInfo(new KakaoOAuth2UserInfo(attributes))
				.build();
	}

	public static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
		return OAuthAttributes.builder()
				.nameAttributeKey(userNameAttributeName)
				.oauth2UserInfo(new NaverOAuth2UserInfo(attributes))
				.build();
	}

    public static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oauth2UserInfo(new GoogleOAuth2UserInfo(attributes))
                .build();
    }

    public Member toEntity(OAuth2UserInfo oauth2UserInfo) {
        return Member.builder()
                .socialType(oauth2UserInfo.getSocialType().toLowerCase(Locale.KOREAN))
                .socialId(oauth2UserInfo.getId())
                .email(oauth2UserInfo.getEmail())
                .imageUrl(oauth2UserInfo.getImageUrl())
                .role(Role.GUEST)
                .build();
    }
}
