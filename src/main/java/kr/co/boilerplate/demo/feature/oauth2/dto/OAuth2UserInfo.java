package kr.co.boilerplate.demo.feature.oauth2.dto;

import java.util.Map;

public abstract class OAuth2UserInfo {

    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

	public abstract String getSocialType();

    public abstract String getId();

    public abstract String getEmail();

    public abstract String getNickname();

    public abstract String getImageUrl();
}
