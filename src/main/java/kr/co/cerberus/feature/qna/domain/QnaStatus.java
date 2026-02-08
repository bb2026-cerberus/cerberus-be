package kr.co.cerberus.feature.qna.domain;

import kr.co.cerberus.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QnaStatus implements BaseEnum {
    PENDING("PENDING", "질문 대기중"),
    ANSWERED("ANSWERED", "답변 완료"),
    CLOSED("CLOSED", "질문 종료");

    private final String code;
    private final String description;
}
