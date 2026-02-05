package kr.co.cerberus.global.common;

import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.global.util.PasswordUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialDataLoader implements CommandLineRunner {
    private final MemberRepository memberRepository;

    public InitialDataLoader(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 멘토 아이디 생성
        createMemberIfNotFound("mentor01", Role.MENTOR);

        //멘티 아이디 생성
        createMemberIfNotFound("mentee01", Role.MENTEE);
        createMemberIfNotFound("mentee02", Role.MENTEE);
    }

    /**
     * 이메일 중복 체크 후 존재하지 않으면 멤버를 저장합니다.
     */
    private void createMemberIfNotFound(String name, Role role) {
        if (memberRepository.findByName(name).isEmpty()) {
            Member member = Member.builder()
                    .name(name)
                    .password("1234")
                    .role(role)
                    .deleteYn("N")
                    .activateYn("Y")
                    .build();

            memberRepository.save(member);
            System.out.println("[InitialData] " + role + " 생성 완료: " + name);
        }
    }
}
