package kr.co.cerberus.feature.feedback.domain;

import kr.co.cerberus.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedbackStatus implements BaseEnum<String> {
    DRAFT("DRAFT", "임시저장"),
    PENDING("PENDING", "피드백 대기중"),
    COMPLETED("COMPLETED", "피드백 완료"),
    CANCELED("CANCELED", "피드백 취소");

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
}
