package kr.co.boilerplate.demo.feature.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

//
@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN("ROLE_ADMIN"), USER("ROLE_USER"), GUEST("ROLE_GUEST");

    private final String key;
}
