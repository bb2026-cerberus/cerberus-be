package kr.co.cerberus.global.common;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import kr.co.cerberus.feature.notification.PushSubscription;
import kr.co.cerberus.feature.notification.repository.PushSubscriptionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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


    private static final LocalDate START_DATE = LocalDate.of(2026, 2, 1);
    private static final LocalDate TODAY = LocalDate.now();

    @Override
    @Transactional
    public void run(String... args) {
        // 회원 (멘토, 멘티) 생성 (고정)
        Member mentor01 = createMemberIfNotFound("mentor01", "김선생", "1234", Role.MENTOR);
        Member mentee01 = createMemberIfNotFound("mentee01", "최학생", "1234", Role.MENTEE);
        Member mentee02 = createMemberIfNotFound("mentee02", "박학생", "1234", Role.MENTEE);

        // Relation 생성: mentor01과 mentee01, mentee02 연결 (고정)
        createRelationIfNotFound(mentor01.getId(), mentee01.getId());
        createRelationIfNotFound(mentor01.getId(), mentee02.getId());

        // Solution (학습지) 생성
	    List<Solution> solutions = createSolutions(mentor01.getId(), mentee01.getId());
	    solutions.addAll(createSolutions(mentor01.getId(), mentee02.getId()));

        // Assignment/Todo 생성
        List<Todo> mentee01Todos = createAssignments(mentee01.getId(), mentor01.getId(), solutions);
        List<Todo> mentee02Todos = createAssignments(mentee02.getId(), mentor01.getId(), solutions);

        // Feedback 생성
        createFeedbacks(mentor01.getId(), mentee01Todos);
        createFeedbacks(mentor01.getId(), mentee02Todos);

        // Q&A 생성
        createQnas(mentor01.getId(), mentee01.getId());
        createQnas(mentor01.getId(), mentee02.getId());

        // WeeklyReport 생성 (지난주: 2026-02-02 월요일 시작 주)
        createWeeklyReports(mentor01.getId(), mentee01.getId(), LocalDate.of(2026, 2, 2));
        createWeeklyReports(mentor01.getId(), mentee02.getId(), LocalDate.of(2026, 2, 2));


        // Push Subscription 생성 (멘티용)
        createPushSubscriptionIfNotFound(mentee01.getId(), "dummy-endpoint-mentee01", "dummy-p256dh", "dummy-auth");
        createPushSubscriptionIfNotFound(mentee02.getId(), "dummy-endpoint-mentee02", "dummy-p256dh", "dummy-auth");

        System.out.println("[InitialData] 모든 초기 데이터 생성 완료.");
    }
    // endpoint/p256dh/auth 값 프론트에서 받기 위해 임시로 하드코딩/ 나중에 프론트랑 붙일때 바꾸기!!!!
    private void createPushSubscriptionIfNotFound(Long menteeId, String endpoint, String p256dh, String auth) {
        boolean exists = pushSubscriptionRepository.findByMenteeId(menteeId).stream()
                .anyMatch(s -> endpoint.equals(s.getEndpoint()));
        if (!exists) {
            pushSubscriptionRepository.save(PushSubscription.builder()
                    .menteeId(menteeId)
                    .endpoint(endpoint)
                    .p256dh(p256dh)
                    .auth(auth)
                    .build());
            System.out.println("[InitialData] PushSubscription 생성 완료 (menteeId=" + menteeId + ")");
        }
    }


    private Member createMemberIfNotFound(String memId, String memName, String password, Role role) {
        return memberRepository.findByMemId(memId).orElseGet(() -> {
            Member member = Member.builder()
                    .memId(memId)
                    .memName(memName)
                    .memPassword(PasswordUtil.encode(password))
                    .role(role)
                    .build();
            memberRepository.save(member);
            System.out.println("[InitialData] " + role + " 생성 완료: " + memId);
            return member;
        });
    }

    private void createRelationIfNotFound(Long mentorId, Long menteeId) {
        boolean exists = relationRepository.findByMentorId(mentorId)
                .stream().anyMatch(r -> r.getMenteeId().equals(menteeId));
        if (!exists) {
            Relation relation = Relation.builder()
                    .mentorId(mentorId)
                    .menteeId(menteeId)
                    .build();
            relationRepository.save(relation);
            System.out.println("[InitialData] 멘토(" + mentorId + ") - 멘티(" + menteeId + ") 관계 생성 완료.");
        }
    }

    private List<Todo> createAssignments(Long menteeId, Long mentorId, List<Solution> solutions) {
        List<Todo> todos = new ArrayList<>();
        Random random = new Random();
        String[] subjects = {"국어", "영어", "수학"};

        for (int i = 1; i <= 15; i++) { // 각 멘티에게 15개씩 과제 부여
            LocalDate todoDate = START_DATE.plusDays(random.nextInt(TODAY.getDayOfYear() - START_DATE.getDayOfYear() + 1));
            String todoCompleteYn = "N";
            String todoAssignYn = "N";
            String todoDraftYn = "N";
            List<FileInfo> todoFiles = new ArrayList<>();
            Long solutionId = null;
            String subject = subjects[random.nextInt(subjects.length)];

            if (i % 3 == 0) { // 완료 상태
                todoCompleteYn = "Y";
                todoAssignYn = "Y";
                todoDraftYn = "Y";
                todoFiles.add(createFileInfo("verification_" + menteeId + "_" + i + ".jpg", "/verifications/" + menteeId + "/" + i + ".jpg", "인증 사진"));
            } else if (i % 3 == 1) { // 할당된 상태 (진행중)
                todoAssignYn = "Y";
                todoDraftYn = "Y";
            } else { // 임시저장 상태
                todoAssignYn = "N";
                todoDraftYn = "N";
            }

            // Solution과 연결 (랜덤하게)
            if (!solutions.isEmpty() && random.nextBoolean()) {
                solutionId = solutions.get(random.nextInt(solutions.size())).getId();
            }

            TodoFileData todoFileData = TodoFileData.withVerificationImages(todoFiles);

            Todo todo = Todo.builder()
                    .menteeId(menteeId)
                    .solutionId(solutionId)
                    .todoDate(todoDate)
                    .todoName(subject + " 과제 " + i)
                    .todoNote(subject + " 과제 내용 " + i + "입니다.")
                    .todoFile(JsonbUtils.toJson(todoFileData))
                    .todoSubjects(subject)
                    .todoAssignYn(todoAssignYn)
                    .todoCompleteYn(todoCompleteYn)
                    .todoDraftYn(todoDraftYn)
                    .build();
            todos.add(todoRepository.save(todo));
        }
        System.out.println("[InitialData] 멘티(" + menteeId + ") 과제/할일 " + todos.size() + "개 생성 완료.");
        return todos;
    }

    private void createFeedbacks(Long mentorId, List<Todo> menteeTodos) {
        Random random = new Random();
        int feedbackCount = 0;
        for (Todo todo : menteeTodos) {
            if ("Y".equals(todo.getTodoCompleteYn()) && random.nextBoolean()) { // 완료된 과제 중 절반 정도만 피드백
                String feedDraftYn = random.nextBoolean() ? "N" : "Y";
                String feedCompleteYn = "N".equals(feedDraftYn) ? "Y" : "N";

                String feedbackContent = "멘토 피드백 내용: " + todo.getTodoName() + "에 대한 상세 피드백입니다.";
                String feedbackSummary = "풀이과정을 자세히 쓰기 (" + todo.getTodoName() + ")";

                Feedback feedback = Feedback.builder()
                        .todoId(todo.getId())
                        .menteeId(todo.getMenteeId())
                        .mentorId(mentorId)
                        .summary(feedbackSummary)
                        .content(feedbackContent)
                        .feedDate(todo.getTodoDate())
                        .feedDraftYn(feedDraftYn)
                        .feedCompleteYn(feedCompleteYn)
                        .build();
                feedbackRepository.save(feedback);
                feedbackCount++;
            }
        }
        System.out.println("[InitialData] 피드백 " + feedbackCount + "개 생성 완료.");
    }

    private void createQnas(Long mentorId, Long menteeId) {
        Random random = new Random();
        for (int i = 1; i <= 3; i++) {
            List<FileInfo> qnaFiles = List.of(createFileInfo("qna_" + menteeId + "_" + i + ".jpg", "/qnas/" + menteeId + "/" + i + ".jpg", "질문 관련 이미지"));
            Qna qna = Qna.builder()
                    .menteeId(menteeId)
                    .mentorId(mentorId)
                    .qnaDate(LocalDate.now().minusDays(i))
                    .questionContent("멘티가 묻습니다: " + i + "번째 질문 내용입니다.")
                    .qnaFile(JsonbUtils.toJson(qnaFiles))
                    .qnaCompleteYn("N")
                    .build();
            Qna savedQna = qnaRepository.save(qna);

            if (random.nextBoolean()) {
                savedQna.updateAnswer("멘토가 답변합니다: " + i + "번째 질문에 대한 답변입니다.");
                qnaRepository.save(savedQna);
            }

            System.out.println("[InitialData] Q&A " + i + "개 생성 완료 (멘티:" + menteeId + ").");
        }
    }

    private void createWeeklyReports(Long mentorId, Long menteeId, LocalDate reportStartDate) {
        WeeklyReport report = WeeklyReport.builder()
                .menteeId(menteeId)
                .mentorId(mentorId)
                .reportDate(reportStartDate)
                .summary("지난 한 주간 멘티(" + menteeId + ")의 학습 요약입니다.")
                .overallEvaluation("전반적으로 성실하게 학습하였으나, 수학 과목에 보완이 필요합니다.")
                .strengths("국어와 영어 과목에서 우수한 성과를 보였습니다.")
                .improvements("수학 개념 이해도를 높이기 위한 추가 학습이 필요합니다.")
                .build();
        weeklyReportRepository.save(report);
        System.out.println("[InitialData] 멘티(" + menteeId + ") 주간 리포트 생성 완료.");
    }

    private FileInfo createFileInfo(String fileName, String fileUrl, String description) {
        return new FileInfo(fileName, fileUrl, description);
    }

    private List<Solution> createSolutions(Long mentorId, Long menteeId) {
		List<Solution> solutions = new ArrayList<>();
        Random random = new Random();
        String[] subjects = {"국어", "영어", "수학"};
        for (int i = 1; i <= 3; i++) {
            String subject = subjects[random.nextInt(subjects.length)];
            FileInfo file = createFileInfo("weakness_solution_" + menteeId + "_" + i + "_file1.pdf", "/weakness_solutions/" + menteeId + "/" + i + "/file1.pdf", "약점 분석 자료");
            
            Solution solution = Solution.builder()
                    .menteeId(menteeId)
                    .mentorId(mentorId)
                    .subject(subject)
                    .solutionContent(subject + " 약점 " + i + " 솔루션: 관련 문제 풀이 및 개념 복습")
                    .solutionFile(JsonbUtils.toJson(file))
                    .build();
            solutionRepository.save(solution);
			
			solutions.add(solution);
        }
        System.out.println("[InitialData] 멘티(" + menteeId + ") 약점 솔루션 3개 생성 완료.");
		return solutions;
    }
}