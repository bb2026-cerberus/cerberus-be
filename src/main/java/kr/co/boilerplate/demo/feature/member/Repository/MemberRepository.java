package kr.co.boilerplate.demo.feature.member.Repository;

import kr.co.boilerplate.demo.feature.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
	
	Optional<Member> findByEmail(String email);
	
	Optional<Member> findBySocialId(String socialId);
	
	Optional<Member> findByRefreshToken(String refreshToken);
	
	Optional<Member> findByDeleteYn(String deleteYn);
	
}