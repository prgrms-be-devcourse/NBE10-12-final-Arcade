package com.back.domain.member.profile.service;

import com.back.domain.member.profile.dtos.CareerCommand;
import com.back.domain.member.profile.dtos.LinkCommand;
import com.back.domain.member.profile.dtos.MemberProfileDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private final MemberProfileRepository memberProfileRepository;

    @Transactional
    public MemberProfileDto me(Member actor) {

        MemberProfile profile = memberProfileRepository.findByMember(actor)
                .orElseGet(() -> memberProfileRepository.save(new MemberProfile(actor)));

        return new MemberProfileDto(profile);

    }

    @Transactional
    public MemberProfileDto modifyProfile(
            Member actor, String nickname, String webpage, String profileImageUrl,
            String bio,
            PositionType position, List<String> techStacks,
            List<CareerCommand> careers, List<LinkCommand> links) {

        MemberProfile profile = memberProfileRepository.findByMember(actor)
                .orElseGet(() -> memberProfileRepository.save(new MemberProfile(actor)));

        profile.modify(nickname, webpage, profileImageUrl, bio,
                position, techStacks, careers, links);

        try {
            memberProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException e) {
            if (isNicknameDuplicate(e)) {
                throw new ServiceException("409-1", "이미 사용 중인 닉네임입니다.");
            }

            throw e;
        }

        return new MemberProfileDto(profile);
    }

    private boolean isNicknameDuplicate(DataIntegrityViolationException e) {
        Throwable mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(e);
        String message = mostSpecificCause.getMessage();

        return message != null
                && message.toLowerCase(Locale.ROOT).contains("uk_member_profile_nickname");
    }
}
