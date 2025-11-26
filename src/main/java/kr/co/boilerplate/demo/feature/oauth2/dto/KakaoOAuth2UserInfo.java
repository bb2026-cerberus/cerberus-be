package kr.co.boilerplate.demo.feature.oauth2.dto;

import kr.co.boilerplate.demo.feature.oauth2.SocialType;

import java.util.Map;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

	@Override
	public String getSocialType() {
		return SocialType.KAKAO.name();
	}

	@Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }

	@Override
	public String getEmail() {
		return "";
	}

	@Override
    public String getNickname() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");

        if (account == null || profile == null) {
            return null;
        }

        return (String) profile.get("nickname");
    }

    @Override
    public String getImageUrl() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");

        if (account == null || profile == null) {
            return null;
        }

        return (String) profile.get("thumbnail_image_url");
    }
}