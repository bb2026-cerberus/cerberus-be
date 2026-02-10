package kr.co.cerberus.feature.feedback.service;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.dto.FeedbackDetailResponseDto;
import kr.co.cerberus.feature.feedback.dto.FeedbackSaveRequestDto;
import kr.co.cerberus.feature.feedback.dto.FeedbackWeeklyBySubjectResponseDto;
import kr.co.cerberus.feature.feedback.dto.FeedbackWeeklyResponseDto;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.notification.service.NotificationService;
import kr.co.cerberus.feature.relation.Relation;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.report.service.WeeklyReportService;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.jsonb.FileInfo;
import kr.co.cerberus.global.jsonb.TodoFileData;
import kr.co.cerberus.global.util.FileStorageService;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final TodoRepository todoRepository;
    private final MemberRepository memberRepository;
    private final RelationRepository relationRepository;
    private final FileStorageService fileStorageService;
    private final ChatClient chatClient;
    private final WeeklyReportService weeklyReportService;
    private final NotificationService notificationService;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            TodoRepository todoRepository,
            MemberRepository memberRepository,
            RelationRepository relationRepository,
            FileStorageService fileStorageService,
            ChatClient.Builder chatClientBuilder,
            WeeklyReportService weeklyReportService,
            NotificationService notificationService) {
        this.feedbackRepository = feedbackRepository;
        this.todoRepository = todoRepository;
        this.memberRepository = memberRepository;
        this.relationRepository = relationRepository;
        this.fileStorageService = fileStorageService;
        this.chatClient = chatClientBuilder.build();
        this.weeklyReportService = weeklyReportService;
        this.notificationService = notificationService;
    }

    /**
     * 피드백 상세 조회 (Todo 정보 포함)
     */
    public FeedbackDetailResponseDto getFeedbackDetail(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "해당 할 일을 찾을 수 없습니다."));
        
        TodoFileData todoFile = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
        Feedback feedback = feedbackRepository.findByTodoIdAndDeleteYn(todoId, "N").orElse(null);

        FeedbackDetailResponseDto.FeedbackInfo feedbackInfo = null;
        if (feedback != null) {
            feedbackInfo = FeedbackDetailResponseDto.FeedbackInfo.builder()
                    .feedbackId(feedback.getId())
                    .content(feedback.getContent())
                    .summary(feedback.getSummary())
                    .draftYn(feedback.getFeedDraftYn())
                    .completeYn(feedback.getFeedCompleteYn())
                    .build();
        }

        return FeedbackDetailResponseDto.builder()
                .todoId(todo.getId())
                .todoName(todo.getTodoName())
                .todoNote(todo.getTodoNote())
                .todoSubjects(todo.getTodoSubjects())
                .todoDate(todo.getTodoDate())
                .verificationImages(todoFile != null && todoFile.getVerificationImages() != null 
                        ? todoFile.getVerificationImages().stream().map(FileInfo::getFileUrl).toList()
                        : Collections.emptyList())
                .feedback(feedbackInfo)
                .build();
    }

    /**
     * 피드백 저장 및 수정
     */
    @Transactional
    public void saveFeedback(FeedbackSaveRequestDto requestDto) {
        Feedback feedback = feedbackRepository.findByTodoIdAndDeleteYn(requestDto.todoId(), "N")
                .orElseGet(() -> Feedback.builder()
                        .todoId(requestDto.todoId())
                        .menteeId(requestDto.menteeId())
                        .mentorId(requestDto.mentorId())
                        .build());

        feedback.updateFeedback(
                requestDto.summary(),
                requestDto.content(),
                "N",
                "Y"
        );

        feedbackRepository.save(feedback);

        notificationService.notifyFeedbackCompleted(requestDto.menteeId(), requestDto.todoId());

    }

    /**
     * [백그라운드] AI Vision 이미지 분석 및 자동 저장
     */
    @Async
    @Transactional
    public void analyzeTodoImagesAsync(Long todoId) {
        log.info("AI 비전 분석 시작 (백그라운드) - Todo ID: {}", todoId);
        
        Todo todo = todoRepository.findById(todoId).orElse(null);
        if (todo == null) return;

        TodoFileData todoFile = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
        if (todoFile == null || todoFile.getVerificationImages() == null || todoFile.getVerificationImages().isEmpty()) {
            return;
        }

        String promptText = """
                당신은 멘토의 피드백 작성을 돕는 전문 교육 에이전트입니다.
                첨부된 이미지는 멘티가 수능 관련 학습을 완료하고 올린 인증 사진입니다.
                이미지를 분석하여 다음 내용을 JSON 형식으로 응답해주세요:
                - summary: 학습 내용에 대한 짧은 요약 (중요 포인트 1문장)
                - content: 멘티의 노력과 성과를 칭찬하고, 보완할 점을 조언하는 정중한 피드백 초안 (3-4문장)
                
                언어는 한국어를 사용하세요.
                """;

        try {
            // Spring AI 1.0.0-M5의 entity()는 Optional이 아닌 객체 자체를 반환함
            Map<String, String> aiResult = chatClient.prompt()
                    .user(u -> {
                        u.text(promptText);
                        todoFile.getVerificationImages().forEach(img -> {
                            try {
                                String url = img.getFileUrl();
                                Resource resource;
                                if (url.startsWith("http")) {
                                    resource = new UrlResource(URI.create(url));
                                } else {
                                    resource = fileStorageService.loadFileAsResource(url);
                                }
                                
                                String contentType = fileStorageService.getContentType(resource);
                                u.media(MimeTypeUtils.parseMimeType(contentType), resource);
                            } catch (Exception e) {
                                log.warn("이미지 리소스 로드 실패: {}, 사유: {}", img.getFileUrl(), e.getMessage());
                            }
                        });
                    })
                    .call()
                    .entity(new ParameterizedTypeReference<>() {});

            if (aiResult != null && !aiResult.isEmpty()) {
				Relation relation = relationRepository.findFirstByMenteeId(todo.getMenteeId());
                Feedback feedback = feedbackRepository.findByTodoIdAndDeleteYn(todoId, "N")
                        .orElseGet(() -> Feedback.builder()
                                .todoId(todoId)
                                .menteeId(todo.getMenteeId())
                                .mentorId(relation.getMentorId())
                                .build());

                feedback.updateFeedback(
                        aiResult.get("summary"),
                        aiResult.get("content"),
                        "N",
                        "N"
                );
                feedbackRepository.save(feedback);
                log.info("AI 비전 분석 완료 및 저장 - Todo ID: {}", todoId);
            }

        } catch (Exception e) {
            log.error("AI 비전 분석 중 에러 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 주간 피드백 목록 조회
     */
    public FeedbackWeeklyResponseDto getWeeklyFeedbacks(Long mentorId, Long menteeId, LocalDate date, String type) {
        LocalDate mondayDate = date.with(DayOfWeek.MONDAY);
        LocalDate sundayDate = mondayDate.plusDays(6);
        
        List<Long> targetMenteeIds = (menteeId != null) 
                ? List.of(menteeId) 
                : relationRepository.findByMentorId(mentorId).stream().map(Relation::getMenteeId).toList();

        if (targetMenteeIds.isEmpty()) {
            return FeedbackWeeklyResponseDto.builder()
                    .weekInfo(calculateWeekInfo(mondayDate))
                    .mondayDate(mondayDate)
                    .groupedItems(Collections.emptyMap())
                    .build();
        }

        Map<Long, String> menteeNameMap = memberRepository.findAllById(targetMenteeIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getMemName));

        List<Todo> todos = todoRepository.findByMenteeIdInAndTodoDateBetweenAndDeleteYn(targetMenteeIds, mondayDate, sundayDate, "N");
        
        if (!"ALL".equalsIgnoreCase(type)) {
            String assignYn = "ASSIGNMENT".equalsIgnoreCase(type) ? "Y" : "N";
            todos = todos.stream()
                    .filter(t -> assignYn.equals(t.getTodoAssignYn()))
                    .toList();
        }

        List<Long> todoIds = todos.stream().map(Todo::getId).toList();
        Map<Long, Feedback> feedbackMap = feedbackRepository.findByTodoIdInAndDeleteYn(todoIds, "N").stream()
                .collect(Collectors.toMap(Feedback::getTodoId, f -> f, (existing, replacement) -> existing));

        List<FeedbackWeeklyResponseDto.TodoWithFeedbackDto> allItems = todos.stream().map(todo -> {
            TodoFileData todoFile = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);
            Feedback feedback = feedbackMap.get(todo.getId());
            
            FeedbackWeeklyResponseDto.FeedbackDetailDto feedbackDto = null;
            if (feedback != null) {
                feedbackDto = FeedbackWeeklyResponseDto.FeedbackDetailDto.builder()
                        .feedbackId(feedback.getId())
                        .content(feedback.getContent())
                        .summary(feedback.getSummary())
                        .draftYn(feedback.getFeedDraftYn())
                        .completeYn(feedback.getFeedCompleteYn())
                        .build();
            }

            return FeedbackWeeklyResponseDto.TodoWithFeedbackDto.builder()
                    .todoId(todo.getId())
                    .menteeId(todo.getMenteeId())
                    .menteeName(menteeNameMap.getOrDefault(todo.getMenteeId(), "알 수 없는 멘티"))
                    .todoName(todo.getTodoName())
                    .todoNote(todo.getTodoNote())
                    .todoSubjects(todo.getTodoSubjects())
                    .todoCompleteYn(todo.getTodoCompleteYn())
                    .todoAssignYn(todo.getTodoAssignYn())
                    .todoDate(todo.getTodoDate())
                    .verificationImages(todoFile != null && todoFile.getVerificationImages() != null 
                        ? todoFile.getVerificationImages().stream().map(FileInfo::getFileUrl).toList()
                        : Collections.emptyList())
                    .feedback(feedbackDto)
                    .build();
        }).toList();

        Map<LocalDate, List<FeedbackWeeklyResponseDto.TodoWithFeedbackDto>> groupedItems = allItems.stream()
                .collect(Collectors.groupingBy(FeedbackWeeklyResponseDto.TodoWithFeedbackDto::todoDate, 
                        TreeMap::new, Collectors.toList()));

        return FeedbackWeeklyResponseDto.builder()
                .weekInfo(calculateWeekInfo(mondayDate))
                .mondayDate(mondayDate)
                .groupedItems(groupedItems)
                .build();
    }

    /**
     * 임시저장 목록 조회
     */
    public List<FeedbackWeeklyResponseDto.TodoWithFeedbackDto> getDraftFeedbacks(Long mentorId) {
        List<Feedback> drafts = feedbackRepository.findByMentorId(mentorId).stream()
                .filter(f -> "Y".equals(f.getFeedDraftYn()) && "N".equals(f.getFeedCompleteYn()))
                .toList();
        
        if (drafts.isEmpty()) return Collections.emptyList();

        List<Long> todoIds = drafts.stream().map(Feedback::getTodoId).toList();
        Map<Long, Todo> todoMap = todoRepository.findAllById(todoIds).stream()
                .collect(Collectors.toMap(Todo::getId, t -> t));
        
        List<Long> menteeIds = drafts.stream().map(Feedback::getMenteeId).distinct().toList();
        Map<Long, String> menteeNameMap = memberRepository.findAllById(menteeIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getMemName));

        return drafts.stream().map(feedback -> {
            Todo todo = todoMap.get(feedback.getTodoId());
            if (todo == null) return null;

            TodoFileData todoFile = JsonbUtils.fromJson(todo.getTodoFile(), TodoFileData.class);

            return FeedbackWeeklyResponseDto.TodoWithFeedbackDto.builder()
                    .todoId(todo.getId())
                    .menteeId(todo.getMenteeId())
                    .menteeName(menteeNameMap.getOrDefault(todo.getMenteeId(), "알 수 없는 멘티"))
                    .todoName(todo.getTodoName())
                    .todoNote(todo.getTodoNote())
                    .todoSubjects(todo.getTodoSubjects())
                    .todoCompleteYn(todo.getTodoCompleteYn())
                    .todoAssignYn(todo.getTodoAssignYn())
                    .todoDate(todo.getTodoDate())
                    .verificationImages(todoFile != null && todoFile.getVerificationImages() != null 
                        ? todoFile.getVerificationImages().stream().map(FileInfo::getFileUrl).toList()
                        : Collections.emptyList())
                    .feedback(FeedbackWeeklyResponseDto.FeedbackDetailDto.builder()
                            .feedbackId(feedback.getId())
                            .content(feedback.getContent())
                            .summary(feedback.getSummary())
                            .draftYn(feedback.getFeedDraftYn())
                            .completeYn(feedback.getFeedCompleteYn())
                            .build())
                    .build();
        }).filter(Objects::nonNull).toList();
    }

    /**
     * 주간 과목별 피드백 목록 조회
     */
    public FeedbackWeeklyBySubjectResponseDto getWeeklyFeedbacksBySubject(Long menteeId, LocalDate date, String type) {
        LocalDate mondayDate = date.with(DayOfWeek.MONDAY);
        Relation relation = relationRepository.findByMenteeIdAndDeleteYn(menteeId, "N");

        FeedbackWeeklyResponseDto weeklyResult = getWeeklyFeedbacks(relation.getMentorId(), menteeId, date, type);
        List<FeedbackWeeklyResponseDto.TodoWithFeedbackDto> allItems = weeklyResult.groupedItems().values().stream()
                .flatMap(List::stream)
                .toList();

        Map<String, List<FeedbackWeeklyBySubjectResponseDto.FeedbackDetailDto>> groupedBySubject = allItems.stream()
                .filter(item -> item.feedback() != null)
                .collect(Collectors.groupingBy(
                        item -> item.todoSubjects() != null ? item.todoSubjects() : "기타",
                        TreeMap::new,
                        Collectors.mapping(item -> {
                            FeedbackWeeklyResponseDto.FeedbackDetailDto fb = item.feedback();
                            return FeedbackWeeklyBySubjectResponseDto.FeedbackDetailDto.builder()
                                    .todoId(item.todoId())
                                    .todoType("Y".equals(item.todoAssignYn()) ? "ASSIGNMENT" : "TODO")
                                    .menteeId(item.menteeId())
                                    .todoSubjects(item.todoSubjects())
                                    .feedbackId(fb.feedbackId())
                                    .content(fb.content())
                                    .summary(fb.summary())
                                    .draftYn(fb.draftYn())
                                    .completeYn(fb.completeYn())
                                    .build();
                        }, Collectors.toList())
                ));

        // 이번 주 피드백 요약
        String feedbackSummary = weeklyReportService.getMenteeWeeklyFeedbackSummary(menteeId, mondayDate);

        return FeedbackWeeklyBySubjectResponseDto.builder()
                .weekInfo(weeklyResult.weekInfo())
                .mondayDate(weeklyResult.mondayDate())
                .summary(feedbackSummary)
                .feedback(groupedBySubject)
                .build();
    }

    private String calculateWeekInfo(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.KOREA);
        int month = date.getMonthValue();
        int weekOfMonth = date.get(weekFields.weekOfMonth());
        return String.format("%d월 %d주차", month, weekOfMonth);
    }
}
