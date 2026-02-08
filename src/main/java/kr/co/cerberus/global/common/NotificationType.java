package kr.co.cerberus.global.common;

import kr.co.cerberus.global.util.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum NotificationType implements BaseEnum<String> {
	FEEDBACK_TODO("FEEDBACK_TODO", "피드백 할일"),
	FEEDBACK_ASSIGNMENT("FEEDBACK_ASSIGNMENT", "피드백 과제"),
	REMIND("REMIND", "리마인드");

	private final String code;
	private final String description;

	@Override
	public String getCode() {
		return this.code;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	private static final Map<String, NotificationType> codeMap = EnumUtils.createByCodeMap(NotificationType.class);

	public static NotificationType of(String code) {
		return EnumUtils.fromCode(NotificationType.class, codeMap, code);
	}
}
