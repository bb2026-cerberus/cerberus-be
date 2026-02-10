package kr.co.cerberus.feature.mentor.dto;

import java.util.List;

public record MenteeManagementDto(
    Long menteeId,
    String menteeName,
    int completedCount,
    int totalCount,
    List<String> unsubmittedTitles,
    String pendingLabel
) {
    // 진행률 계산 메서드 (옵션)
    public double getProgressRate() {
        if (totalCount == 0) return 0.0;
        return (double) completedCount / totalCount * 100.0;
    }
}
