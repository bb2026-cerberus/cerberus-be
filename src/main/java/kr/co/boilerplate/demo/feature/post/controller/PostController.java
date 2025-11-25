package kr.co.boilerplate.demo.feature.post.controller;

import kr.co.boilerplate.demo.global.annotation.LogExecutionTime;
import kr.co.boilerplate.demo.global.common.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

	@LogExecutionTime
	@GetMapping
	public ResponseEntity<CommonResponse> getPosts() {
		return ResponseEntity.ok(null);
	}
}
