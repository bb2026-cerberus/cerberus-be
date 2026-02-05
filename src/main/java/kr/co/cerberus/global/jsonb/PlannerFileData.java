package kr.co.cerberus.global.jsonb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * tb_planner.plan_file JSONB 구조
 *
 * {
 *   "imageUrl": "/files/301",
 *   "question": "질문이 있어요."
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerFileData {
	private String imageUrl;
	private String question;

	public PlannerFileData withImageUrl(String imageUrl) {
		return PlannerFileData.builder()
				.imageUrl(imageUrl)
				.question(this.question)
				.build();
	}

	public PlannerFileData withQuestion(String question) {
		return PlannerFileData.builder()
				.imageUrl(this.imageUrl)
				.question(question)
				.build();
	}
}
