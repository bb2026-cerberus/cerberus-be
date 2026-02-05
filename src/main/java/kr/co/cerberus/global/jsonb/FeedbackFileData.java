package kr.co.cerberus.global.jsonb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tb_feedback.feed_file JSONB 구조
 *
 * {
 *   "content": "꼼꼼하게 잘 풀었어요. 15번 문제는 다시 한번 확인해보세요.",
 *   "summary": "풀이과정을 자세히 쓰기",
 *   "files": [{"fileName": "첨삭.pdf", "fileUrl": "/files/401"}]
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackFileData {
	private String content;
	private String summary;
	private List<FileInfo> files;
}
