package kr.co.cerberus.global.common;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.notification.PushSubscription;
import kr.co.cerberus.feature.notification.repository.PushSubscriptionRepository;
import kr.co.cerberus.feature.qna.Qna;
import kr.co.cerberus.feature.qna.repository.QnaRepository;
import kr.co.cerberus.feature.relation.Relation;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.report.WeeklyReport;
import kr.co.cerberus.feature.report.repository.WeeklyReportRepository;
import kr.co.cerberus.feature.solution.Solution;
import kr.co.cerberus.feature.solution.repository.SolutionRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.jsonb.FileInfo;
import kr.co.cerberus.global.jsonb.TodoFileData;
import kr.co.cerberus.global.util.JsonbUtils;
import kr.co.cerberus.global.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialDataLoader implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final RelationRepository relationRepository;
    private final SolutionRepository solutionRepository;
    private final TodoRepository todoRepository;
    private final FeedbackRepository feedbackRepository;
    private final QnaRepository qnaRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 2, 10);
    private static final String DEFAULT_IMG_URL = "/home/blisle/uploads/todos/img.png";
    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[InitialData] 데이터 로딩 시작...");

        // 1. 회원 및 관계 생성
        Member mentor = createMemberIfNotFound("mentor01", "김선생", "1234", Role.MENTOR);
        Member mentee1 = createMemberIfNotFound("mentee01", "최학생", "1234", Role.MENTEE);
        Member mentee2 = createMemberIfNotFound("mentee02", "박학생", "1234", Role.MENTEE);

        createRelationIfNotFound(mentor.getId(), mentee1.getId());
        createRelationIfNotFound(mentor.getId(), mentee2.getId());

        // 2. 솔루션(학습지) 풀 정의 및 할당
        // 멘토가 가진 5개의 솔루션 정의
        SolutionDef[] solutionPool = {
                new SolutionDef("수학", "미적분 기초 다지기", "함수의 극한과 연속 단원 집중 풀이"),
                new SolutionDef("수학", "확률과 통계 심화", "조건부 확률 기출 문제 분석"),
                new SolutionDef("국어", "비문학 독해 전략", "과학/경제 지문 구조 분석 및 요약"),
                new SolutionDef("영어", "수능 필수 구문 독해", "복합 관계사 및 가정법 구문 마스터"),
        };

        // 멘티1: 4개 할당 (0, 1, 2, 3)
        List<Solution> mentee1Solutions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            mentee1Solutions.add(saveSolution(mentor.getId(), mentee1.getId(), solutionPool[i]));
        }

        // 멘티2: 3개 할당 (2, 3, 4) -> 2, 3번이 공통 솔루션
        List<Solution> mentee2Solutions = new ArrayList<>();
        for (int i = 2; i < 4; i++) {
            mentee2Solutions.add(saveSolution(mentor.getId(), mentee2.getId(), solutionPool[i]));
        }

        // 3. 일별 Todo 및 관련 데이터 생성 (2026-01-01 ~ 2026-02-10)
        for (LocalDate date = START_DATE; !date.isAfter(END_DATE); date = date.plusDays(1)) {
            createDailyData(date, mentor, mentee1, mentee1Solutions);
            createDailyData(date, mentor, mentee2, mentee2Solutions);
        }

        // 4. 주간 리포트 생성 (예시로 몇 개만 생성)
        createWeeklyReports(mentor.getId(), mentee1.getId(), LocalDate.of(2026, 2, 2));
        createWeeklyReports(mentor.getId(), mentee2.getId(), LocalDate.of(2026, 2, 2));

        // 5. 푸시 구독 정보
        createPushSubscriptionIfNotFound(mentee1.getId(), "endpoint-mentee1", "p256dh", "auth");
        createPushSubscriptionIfNotFound(mentee2.getId(), "endpoint-mentee2", "p256dh", "auth");

        log.info("[InitialData] 모든 초기 데이터 생성 완료 (2026-01-01 ~ 2026-02-10)");
    }

    private void createDailyData(LocalDate date, Member mentor, Member mentee, List<Solution> solutions) {
        // 과제 (Mentor-created): 2~3개
        int assignmentCount = 2 + random.nextInt(2);
        for (int i = 1; i <= assignmentCount; i++) {
            Solution sol = solutions.get(random.nextInt(solutions.size()));
            boolean isSpecificDate = date.equals(LocalDate.of(2026, 2, 9));
            
            // 02-09일에는 일부 과제를 완료상태로 만들되 피드백은 생성하지 않음
            boolean forceNoFeedback = isSpecificDate && i == 1; 
            boolean isComplete = !date.equals(END_DATE) && (random.nextDouble() < 0.8 || forceNoFeedback);

            Todo assignment = Todo.builder()
                    .menteeId(mentee.getId())
                    .solutionId(sol.getId())
                    .todoDate(date)
                    .todoName(sol.getSubject() + " 과제: " + sol.getSolutionContent().split(" ")[0] + " 외")
                    .todoNote(sol.getSolutionContent() + " 에 따른 일일 과제입니다.")
                    .todoSubjects(sol.getSubject())
                    .todoAssignYn("Y")
                    .todoCompleteYn(isComplete ? "Y" : "N")
                    .todoDraftYn("N")
                    .todoFile(isComplete ? createVerificationJson() : null)
                    .build();
            todoRepository.save(assignment);

            // 피드백 생성 (완료된 과제에 대해, 02-09 강제 제외 케이스 외)
            if (isComplete && !forceNoFeedback && random.nextDouble() < 0.9) {
                saveFeedback(mentor.getId(), mentee.getId(), assignment, date);
            }
        }

        // 할 일 (Mentee-created): 3~4개
        int todoCount = 3 + random.nextInt(2);
        String[] subjects = {"국어", "수학", "영어"};
        for (int i = 1; i <= todoCount; i++) {
            String subject = subjects[random.nextInt(subjects.length)];
            boolean isComplete = !date.equals(END_DATE) && random.nextDouble() < 0.7;

            Todo todo = Todo.builder()
                    .menteeId(mentee.getId())
                    .todoDate(date)
                    .todoName(subject + " 개인 학습 " + i)
                    .todoNote(subject + " 단원 정리 및 문제 풀이")
                    .todoSubjects(subject)
                    .todoAssignYn("N")
                    .todoCompleteYn(isComplete ? "Y" : "N")
                    .todoDraftYn("N")
                    .build();
            todoRepository.save(todo);
        }

        // 02-10 (오늘)에는 임시저장(Draft) 데이터를 추가
        if (date.equals(END_DATE)) {
            // 멘토의 임시저장 과제 (출제 중)
            Solution sol = solutions.get(random.nextInt(solutions.size()));
            todoRepository.save(Todo.builder()
                    .menteeId(mentee.getId())
                    .solutionId(sol.getId())
                    .todoDate(date)
                    .todoName("[임시저장] " + sol.getSubject() + " 심화 보충 과제")
                    .todoNote("멘토가 내용을 다듬고 있는 임시저장 과제입니다.")
                    .todoSubjects(sol.getSubject())
                    .todoAssignYn("N")
                    .todoCompleteYn("N")
                    .todoDraftYn("Y")
                    .build());

            // 멘티의 임시저장 할 일 (계획 중)
            todoRepository.save(Todo.builder()
                    .menteeId(mentee.getId())
                    .todoDate(date)
                    .todoName("[임시저장] 이번 주말 오답 정리 계획")
                    .todoNote("멘티가 작성 중인 임시저장 할 일입니다.")
                    .todoSubjects("자습")
                    .todoAssignYn("N")
                    .todoCompleteYn("N")
                    .todoDraftYn("Y")
                    .build());
        }

        // Q&A 생성 (0~1개)
        if (random.nextDouble() < 0.3 || (date.equals(LocalDate.of(2026, 2, 9)) && mentee.getMemId().equals("mentee01"))) {
            boolean isUnanswered = date.equals(LocalDate.of(2026, 2, 9));
            Qna qna = Qna.builder()
                    .menteeId(mentee.getId())
                    .mentorId(mentor.getId())
                    .qnaDate(date)
                    .questionContent(date + " 학습 중 질문입니다. 이 부분이 이해가 잘 안 가요.")
                    .qnaCompleteYn(isUnanswered ? "N" : "Y")
                    .answerContent(isUnanswered ? null : "해당 부분은 기본 개념서를 다시 참고하면 도움이 될 거예요.")
                    .build();
            qnaRepository.save(qna);
        }
    }

    private String createVerificationJson() {
        List<FileInfo> files = List.of(new FileInfo("img.png", DEFAULT_IMG_URL, "과제 인증 사진"));
        return JsonbUtils.toJson(TodoFileData.withVerificationImages(files));
    }

    private Solution saveSolution(Long mentorId, Long menteeId, SolutionDef def) {
        Solution solution = Solution.builder()
                .mentorId(mentorId)
                .menteeId(menteeId)
                .subject(def.subject)
                .solutionContent(def.title + " - " + def.content)
                .solutionFile(JsonbUtils.toJson(new FileInfo("solution.pdf", "/home/blisle/uploads/solutions/sample.pdf", "학습 가이드")))
                .build();
        return solutionRepository.save(solution);
    }

    private void saveFeedback(Long mentorId, Long menteeId, Todo todo, LocalDate date) {
        Feedback feedback = Feedback.builder()
                .todoId(todo.getId())
                .menteeId(menteeId)
                .mentorId(mentorId)
                .feedDate(date)
                .summary("성실한 학습이 돋보입니다.")
                .content("풀이 과정이 아주 깔끔하네요. 다음 단계로 넘어가도 좋겠습니다.")
                .feedDraftYn("N")
                .feedCompleteYn("Y")
                .build();
        feedbackRepository.save(feedback);
    }

    private Member createMemberIfNotFound(String memId, String memName, String password, Role role) {
        return memberRepository.findByMemId(memId).orElseGet(() -> {
            Member member = Member.builder()
                    .memId(memId)
                    .memName(memName)
                    .memPassword(PasswordUtil.encode(password))
                    .role(role)
                    .build();
            return memberRepository.save(member);
        });
    }

    private void createRelationIfNotFound(Long mentorId, Long menteeId) {
        boolean exists = relationRepository.findByMentorId(mentorId).stream()
                .anyMatch(r -> r.getMenteeId().equals(menteeId));
        if (!exists) {
            relationRepository.save(Relation.builder().mentorId(mentorId).menteeId(menteeId).build());
        }
    }

    private void createWeeklyReports(Long mentorId, Long menteeId, LocalDate startDate) {
        WeeklyReport report = WeeklyReport.builder()
                .menteeId(menteeId)
                .mentorId(mentorId)
                .reportDate(startDate)
                .summary("2월 1주차 학습 리포트입니다.")
                .overallEvaluation("전반적으로 계획에 맞춰 학습을 잘 진행하고 있습니다.")
                .strengths("과제 이행률이 매우 높습니다.")
                .improvements("오답 노트를 좀 더 상세히 작성하면 좋겠습니다.")
                .build();
        weeklyReportRepository.save(report);
    }

    private void createPushSubscriptionIfNotFound(Long menteeId, String endpoint, String p256dh, String auth) {
        if (pushSubscriptionRepository.findByMenteeId(menteeId).isEmpty()) {
            pushSubscriptionRepository.save(PushSubscription.builder()
                    .menteeId(menteeId).endpoint(endpoint).p256dh(p256dh).auth(auth).build());
        }
    }

    private record SolutionDef(String subject, String title, String content) {}
}
