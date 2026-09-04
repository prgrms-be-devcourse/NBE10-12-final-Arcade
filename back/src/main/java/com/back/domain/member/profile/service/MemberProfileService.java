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
            String bio, String githubUsername,
            PositionType position, List<String> techStacks,
            List<CareerCommand> careers, List<LinkCommand> links) {

        MemberProfile profile = memberProfileRepository.findByMember(actor)
                .orElseGet(() -> memberProfileRepository.save(new MemberProfile(actor)));

        profile.modify(nickname, webpage, profileImageUrl, bio, githubUsername,
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

    /** 저장만 하고 URL 을 돌려준다. 프로필에 반영하는 건 수정 요청의 몫이다. */
    public String uploadProfileImage(MultipartFile file) {
        validateImage(file);

        return fileStorage.upload(file, PROFILE_IMAGE_DIRECTORY);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("400-1", "이미지 파일이 비어 있습니다.");
        }

        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
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
