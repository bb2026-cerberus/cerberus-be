package kr.co.boilerplate.demo.feature.oauth2.dto;

import kr.co.boilerplate.demo.feature.oauth2.SocialType;

import java.util.Map;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

	@Override
	public String getSocialType() {
		return SocialType.GOOGLE.name();
	}

	@Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {return (String) attributes.get("email"); }

    @Override
    public String getNickname() {
        return (String) attributes.get("name");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}
