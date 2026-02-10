package kr.co.cerberus.feature.mentor.dto;

import java.util.List;

public record MenteeListResponseDto(
    List<MenteeDetailItem> mentees
) {
    public record MenteeDetailItem(
        Long menteeId,
        String menteeName,
        MenteeDetailsResponseDto details
    ) {}
}
