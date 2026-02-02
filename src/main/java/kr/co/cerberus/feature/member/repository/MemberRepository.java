package kr.co.cerberus.feature.member.repository;

import kr.co.cerberus.feature.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
	
	Optional<Member> findByEmail(String email);
	
	Optional<Member> findByDeleteYn(String deleteYn);
	
}