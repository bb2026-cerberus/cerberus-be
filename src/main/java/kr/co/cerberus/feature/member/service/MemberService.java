package kr.co.cerberus.feature.member.service;

import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.dto.LoginResponseDto;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.cerberus.global.util.PasswordUtil;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public LoginResponseDto login(String memId, String password) {
        Member member = memberRepository.findByMemId(memId).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (!PasswordUtil.matches(password, member.getMemPassword())) {
            throw new CustomException(ErrorCode.LOCAL_LOGIN_FAIL);
        }

        return LoginResponseDto.from(member);
    }
}

