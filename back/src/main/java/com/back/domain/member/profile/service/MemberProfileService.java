package com.back.domain.member.profile.service;

import com.back.domain.member.profile.dtos.CareerCommand;
import com.back.domain.member.profile.dtos.LinkCommand;
import com.back.domain.member.profile.dtos.MemberProfileDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.global.app.CustomConfigProperties;
import com.back.global.exception.ServiceException;
import com.back.global.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private static final String PROFILE_IMAGE_DIRECTORY = "profile";
    private static final List<String> ALLOWED_IMAGE_TYPES =
            List.of("image/jpeg", "image/png");

    private final MemberProfileRepository memberProfileRepository;
    private final FileStorage fileStorage;
    private final CustomConfigProperties customConfigProperties;

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

    /**
     * 대표 포지션만 바꾼다. GitHub 로 처음 가입하면 닉네임이 없어
     * nickname 이 필수인 modifyProfile 로는 포지션을 저장할 수 없다.
     */
    @Transactional
    public MemberProfileDto modifyPosition(Member actor, PositionType position) {
        MemberProfile profile = memberProfileRepository.findByMember(actor)
                .orElseGet(() -> memberProfileRepository.save(new MemberProfile(actor)));

        profile.changePosition(position);

        return new MemberProfileDto(profile);
    }

    /** 저장만 하고 URL 을 돌려준다. 프로필에 반영하는 건 수정 요청의 몫이다. */
    public String uploadProfileImage(MultipartFile file) {
        validateImage(file);

        return fileStorage.upload(file, PROFILE_IMAGE_DIRECTORY);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("400-1", "이미지 파일이 비어 있습니다.");
        }

        // Content-Type 헤더가 없는 파트면 getContentType() 이 null 이다.
        // List.of() 로 만든 목록은 contains(null) 에서 NPE 를 내므로 먼저 거른다.
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ServiceException("400-1", "jpg, png 이미지만 올릴 수 있습니다.");
        }

        long maxBytes = customConfigProperties.getStorage().getMaxFileSize().toBytes();

        if (file.getSize() > maxBytes) {
            throw new ServiceException("400-1",
                    "이미지는 %dMB 까지 올릴 수 있습니다.".formatted(maxBytes / 1024 / 1024));
        }
    }

    private boolean isNicknameDuplicate(DataIntegrityViolationException e) {
        Throwable mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(e);
        String message = mostSpecificCause.getMessage();

        return message != null
                && message.toLowerCase(Locale.ROOT).contains("uk_member_profile_nickname");
    }
}
