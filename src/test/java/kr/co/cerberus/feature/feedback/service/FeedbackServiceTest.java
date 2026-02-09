package kr.co.cerberus.feature.feedback.service;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.jsonb.TodoFileData;
import kr.co.cerberus.global.util.JsonbUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private ChatClient chatClient;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    @DisplayName("AI Vision 분석 후 결과가 임시저장 상태로 DB에 저장되어야 한다")
    void analyzeTodoImagesAsync_Success() {
        // given
        Long todoId = 1L;
        Todo mockTodo = Todo.builder()
                .id(todoId)
                .menteeId(100L)
                .todoFile(JsonbUtils.toJson(TodoFileData.builder()
                        .verificationImages(Collections.singletonList(new kr.co.cerberus.global.jsonb.FileInfo("test.jpg", "http://test.com/img.jpg")))
                        .build()))
                .build();

        when(todoRepository.findById(todoId)).thenReturn(Optional.of(mockTodo));
        when(feedbackRepository.findByTodoIdAndDeleteYn(todoId, "N")).thenReturn(Optional.empty());

        // ChatClient Fluent API Mocking
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        
        Map<String, String> aiResult = Map.of(
                "summary", "학습 내용 요약입니다.",
                "content", "칭찬과 조언이 섞인 피드백입니다."
        );
        when(responseSpec.entity(any(ParameterizedTypeReference.class))).thenReturn(aiResult);

        // when
        // 비동기 메서드지만 단위 테스트에서는 직접 호출하여 동기적으로 실행됨을 검증
        feedbackService.analyzeTodoImagesAsync(todoId);

        // then
        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository, times(1)).save(feedbackCaptor.capture());

        Feedback savedFeedback = feedbackCaptor.getValue();
        assertThat(savedFeedback.getTodoId()).isEqualTo(todoId);
        assertThat(savedFeedback.getFeedDraftYn()).isEqualTo("Y");
        assertThat(savedFeedback.getFeedCompleteYn()).isEqualTo("N");
        
        String feedFile = savedFeedback.getFeedFile();
        assertThat(feedFile).contains("학습 내용 요약입니다.");
        assertThat(feedFile).contains("칭찬과 조언이 섞인 피드백입니다.");
    }
}
