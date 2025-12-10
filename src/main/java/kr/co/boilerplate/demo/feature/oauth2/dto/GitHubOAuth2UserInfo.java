package kr.co.boilerplate.demo.feature.oauth2.dto;

import java.util.Map;

public class GitHubOAuth2UserInfo extends OAuth2UserInfo {

	public GitHubOAuth2UserInfo(Map<String, Object> attributes) {
		super(attributes);
	}

	@Override
	public String getSocialType() {
		return "";
	}

	@Override
	public String getId() {
		return "";
	}

	@Override
	public String getEmail() {
		return "";
	}

	@Override
	public String getNickname() {
		return "";
	}

	@Override
	public String getImageUrl() {
		return "";
	}
}
