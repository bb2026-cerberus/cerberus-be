package kr.co.cerberus.global.common;

import kr.co.cerberus.feature.assignment.domain.AssignmentStatus;
import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.domain.FeedbackStatus;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.global.jsonb.FeedbackFileData;
import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.qna.Qna;
import kr.co.cerberus.feature.qna.domain.QnaStatus;
import kr.co.cerberus.feature.qna.repository.QnaRepository;
import kr.co.cerberus.feature.relation.Relation;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.report.WeeklyReport;
import kr.co.cerberus.feature.report.repository.WeeklyReportRepository;
import kr.co.cerberus.feature.solution.Solution;
import kr.co.cerberus.feature.solution.repository.SolutionRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.feature.weakness.WeaknessSolution; // Add this import
import kr.co.cerberus.feature.weakness.repository.WeaknessSolutionRepository;
import kr.co.cerberus.global.jsonb.FileInfo;
import kr.co.cerberus.global.jsonb.TodoFileData;
import kr.co.cerberus.global.util.JsonbUtils;
import kr.co.cerberus.global.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final WeaknessSolutionRepository weaknessSolutionRepository;    private static final LocalDate START_DATE = LocalDate.of(2026, 2, 1);
    private static final LocalDate TODAY = LocalDate.now();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. 회원 (멘토, 멘티) 생성
        Member mentor01 = createMemberIfNotFound("mentor01", "1234", Role.MENTOR);
        Member mentee01 = createMemberIfNotFound("mentee01", "1234", Role.MENTEE);
        Member mentee02 = createMemberIfNotFound("mentee02", "1234", Role.MENTEE);

        // 2. Relation 생성: mentor01과 mentee01, mentee02 연결
        createRelationIfNotFound(mentor01.getId(), mentee01.getId());
        createRelationIfNotFound(mentor01.getId(), mentee02.getId());

        // 3. Solution (학습지) 생성
        List<Solution> solutions = createSolutions(mentor01.getId());

        // 4. Assignment/Todo 생성
        List<Todo> mentee01Todos = createAssignments(mentee01.getId(), mentor01.getId(), solutions);
        List<Todo> mentee02Todos = createAssignments(mentee02.getId(), mentor01.getId(), solutions);

        // 5. Feedback 생성
        createFeedbacks(mentor01.getId(), mentee01Todos);
        createFeedbacks(mentor01.getId(), mentee02Todos);

        // 6. Q&A 생성
        createQnas(mentor01.getId(), mentee01.getId());
        createQnas(mentor01.getId(), mentee02.getId());

        // 7. WeeklyReport 생성 (지난주: 2026-02-02 월요일 시작 주)
        createWeeklyReports(mentor01.getId(), mentee01.getId(), LocalDate.of(2026, 2, 2));
        createWeeklyReports(mentor01.getId(), mentee02.getId(), LocalDate.of(2026, 2, 2));

        // 8. WeaknessSolution 생성
        createWeaknessSolutions(mentor01.getId(), mentee01.getId());
        createWeaknessSolutions(mentor01.getId(), mentee02.getId());

        System.out.println("[InitialData] 모든 초기 데이터 생성 완료.");
    }

    private Member createMemberIfNotFound(String name, String password, Role role) {
        return memberRepository.findByName(name).orElseGet(() -> {
            Member member = Member.builder()
                    .name(name)
                    .password(PasswordUtil.encode(password))
                    .role(role)
                    .build();
            memberRepository.save(member);
            System.out.println("[InitialData] " + role + " 생성 완료: " + name);
            return member;
        });
    }

    private void createRelationIfNotFound(Long mentorId, Long menteeId) {
        boolean exists = relationRepository.findByMentorIdAndActivateYn(mentorId, "Y")
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

    private List<Solution> createSolutions(Long mentorId) {
        List<Solution> solutions = new ArrayList<>();
        String[] subjects = {"국어", "영어", "수학"};
        Random random = new Random();

        for (String subject : subjects) {
            for (int i = 1; i <= 5; i++) {
                List<FileInfo> files = List.of(
                        createFileInfo("solution_" + subject + "_" + i + "_file1.pdf", "/solutions/" + subject + "/" + i + "/file1.pdf", "메인 학습 자료"),
                        createFileInfo("solution_" + subject + "_" + i + "_file2.jpg", "/solutions/" + subject + "/" + i + "/file2.jpg", "참고 이미지")
                );
                Solution solution = Solution.builder()
                        .mentorId(mentorId)
                        .title(subject + " 기본 개념 학습지 " + i)
                        .description(subject + " 과목의 기본 개념을 다지는 학습지 솔루션입니다.")
                        .subject(subject)
                        .solutionFile(JsonbUtils.toJson(files))
                        .build();
                solutions.add(solutionRepository.save(solution));
            }
        }
        System.out.println("[InitialData] 솔루션 " + solutions.size() + "개 생성 완료.");
        return solutions;
    }

    private List<Todo> createAssignments(Long menteeId, Long mentorId, List<Solution> solutions) {
        List<Todo> todos = new ArrayList<>();
        Random random = new Random();
        String[] subjects = {"국어", "영어", "수학"};

        for (int i = 1; i <= 15; i++) { // 각 멘티에게 15개씩 과제 부여
            LocalDate todoDate = START_DATE.plusDays(random.nextInt(TODAY.getDayOfYear() - START_DATE.getDayOfYear() + 1));
            AssignmentStatus status;
            String todoCompleteYn = "N";
            List<FileInfo> todoFiles = new ArrayList<>();
            Long solutionId = null;
            String subject = subjects[random.nextInt(subjects.length)];

            if (i % 3 == 0) { // 3의 배수는 완료
                status = AssignmentStatus.COMPLETED;
                todoCompleteYn = "Y";
                todoFiles.add(createFileInfo("verification_" + menteeId + "_" + i + ".jpg", "/verifications/" + menteeId + "/" + i + ".jpg", "인증 사진"));
            } else if (i % 3 == 1) { // 3으로 나눈 나머지가 1은 진행중
                status = AssignmentStatus.IN_PROGRESS;
            } else { // 나머지는 임시저장
                status = AssignmentStatus.DRAFT;
            }

            // Solution과 연결 (랜덤하게)
            if (!solutions.isEmpty() && random.nextBoolean()) {
                solutionId = solutions.get(random.nextInt(solutions.size())).getId();
            }

            TodoFileData todoFileData = new TodoFileData(todoFiles, (todoFiles.isEmpty() ? null : todoFiles.get(0).getFileUrl()));

            Todo todo = Todo.builder()
                    .menteeId(menteeId)
                    .goalId(1L + random.nextInt(3)) // 가상의 목표 ID
                    .solutionId(solutionId)
                    .todoDate(todoDate)
                    .todoName(subject + " 과제 " + i + " (" + status.getDescription() + ")")
                    .todoNote(subject + " 과제 내용 " + i + "입니다.")
                    .todoFile(JsonbUtils.toJson(todoFileData))
                    .todoSubjects(subject)
                    .todoAssignYn("Y") // 과제는 항상 'Y'
                    .todoCompleteYn(todoCompleteYn)
                    .status(status)
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
            if (todo.getStatus() == AssignmentStatus.COMPLETED && random.nextBoolean()) { // 완료된 과제 중 절반 정도만 피드백
                FeedbackStatus status = random.nextBoolean() ? FeedbackStatus.COMPLETED : FeedbackStatus.DRAFT;
                List<FileInfo> feedbackFiles = List.of(
                        createFileInfo("feedback_" + todo.getId() + "_file.pdf", "/feedbacks/" + todo.getId() + "/file.pdf", "피드백 첨부 자료")
                );
                String feedbackContent = "멘토 피드백 내용: " + todo.getTodoName() + "에 대한 상세 피드백입니다. (" + status.getDescription() + ")";
                String feedbackSummary = "풀이과정을 자세히 쓰기 (" + todo.getTodoName() + ")"; // Add summary
                FeedbackFileData feedbackData = new FeedbackFileData(
                        feedbackContent,
                        feedbackSummary, // Pass summary
                        feedbackFiles
                );
                Feedback feedback = Feedback.builder()
                        .todoId(todo.getId())
                        .menteeId(todo.getMenteeId())
                        .mentorId(mentorId)
                        .feedFile(JsonbUtils.toJson(feedbackData))
                        .feedDate(todo.getTodoDate().plusDays(1)) // 과제 다음날 피드백
                        .status(status)
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
                    .title("Q&A 질문 " + i + "입니다.")
                    .questionContent("멘티가 묻습니다: " + i + "번째 질문 내용입니다.")
                    .qnaFile(JsonbUtils.toJson(qnaFiles))
                    .status(QnaStatus.PENDING)
                    .build();
            Qna savedQna = qnaRepository.save(qna); // Assign to savedQna

            // 답변 추가 (랜덤하게 답변되도록 설정)
            if (random.nextBoolean()) {
                savedQna.updateAnswer("멘토가 답변합니다: " + i + "번째 질문에 대한 답변입니다.");
                savedQna.updateStatus(QnaStatus.ANSWERED);
                qnaRepository.save(savedQna); // 최종 상태로 한 번만 저장
            }

            System.out.println("[InitialData] Q&A " + i + "개 생성 완료 (멘티:" + menteeId + ").");
        }
    }

    private void createWeeklyReports(Long mentorId, Long menteeId, LocalDate reportStartDate) {
        // 지난주 월요일 (2026-02-02) 기준 리포트
        List<FileInfo> reportFiles = List.of(createFileInfo("weekly_report_" + menteeId + ".pdf", "/reports/" + menteeId + "/weekly_report.pdf", "주간 요약 파일"));
        WeeklyReport report = WeeklyReport.builder()
                .menteeId(menteeId)
                .mentorId(mentorId)
                .reportDate(reportStartDate)
                .summary("지난 한 주간 멘티(" + menteeId + ")의 학습 요약입니다.")
                .overallEvaluation("전반적으로 성실하게 학습하였으나, 수학 과목에 보완이 필요합니다.")
                .strengths("국어와 영어 과목에서 우수한 성과를 보였습니다.")
                .improvements("수학 개념 이해도를 높이기 위한 추가 학습이 필요합니다.")
                .reportFile(JsonbUtils.toJson(reportFiles))
                .build();
        weeklyReportRepository.save(report);
        System.out.println("[InitialData] 멘티(" + menteeId + ") 주간 리포트 생성 완료.");
    }

    private FileInfo createFileInfo(String fileName, String fileUrl, String description) {
        return new FileInfo(fileName, fileUrl, description);
    }

    private void createWeaknessSolutions(Long mentorId, Long menteeId) {
        Random random = new Random();
        String[] subjects = {"국어", "영어", "수학"};
        for (int i = 1; i <= 3; i++) { // 각 멘티에게 3개씩 약점 솔루션 부여
            String subject = subjects[random.nextInt(subjects.length)];
            List<FileInfo> files = List.of(
                    createFileInfo("weakness_solution_" + menteeId + "_" + i + "_file1.pdf", "/weakness_solutions/" + menteeId + "/" + i + "/file1.pdf", "약점 분석 자료"),
                    createFileInfo("weakness_solution_" + menteeId + "_" + i + "_file2.jpg", "/weakness_solutions/" + menteeId + "/" + i + "/file2.jpg", "보완 학습 자료")
            );
            WeaknessSolution weaknessSolution = WeaknessSolution.builder()
                    .menteeId(menteeId)
                    .mentorId(mentorId)
                    .subject(subject)
                    .weaknessDescription(subject + " 약점 " + i + " 분석: " + subject + " 기본 개념 부족")
                    .solutionContent(subject + " 약점 " + i + " 솔루션: 관련 문제 풀이 및 개념 복습")
                    .solutionFile(JsonbUtils.toJson(files))
                    .build();
            weaknessSolutionRepository.save(weaknessSolution);
        }
        System.out.println("[InitialData] 멘티(" + menteeId + ") 약점 솔루션 " + 3 + "개 생성 완료.");
    }
}
