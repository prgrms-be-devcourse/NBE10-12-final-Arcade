package com.back.domain.member.profile.service;

import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.service.GoalService;
import com.back.domain.member.profile.dtos.CareerCommand;
import com.back.domain.member.profile.dtos.LinkCommand;
import com.back.domain.member.profile.dtos.MemberProfileDto;
import com.back.domain.member.profile.dtos.MemberPublicProfileDto;
import com.back.domain.member.profile.dtos.MemberShowcaseDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.global.app.CustomConfigProperties;
import com.back.global.exception.ServiceException;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
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
    private final MemberRepository memberRepository;
    private final MemberSummaryService memberSummaryService;
    private final PartyShowcaseRepository partyShowcaseRepository;
    private final GoalService goalService;
    private final FileStorage fileStorage;
    private final CustomConfigProperties customConfigProperties;

    @Transactional
    public MemberProfileDto me(Member actor) {

        return new MemberProfileDto(getOrCreateProfile(actor));

    }

    /**
     * 남이 보는 공개 프로필. 지원자 심사·전시에서 이름을 눌렀을 때 열린다.
     *
     * GET /me 와 달리 프로필 행을 만들지 않는다 - 로그인 없이 열리는 경로라 남의 조회가 남의 행을 쓰면 안 된다.
     * 한 번도 프로필을 연 적 없는 회원은 회원 정보만으로 빈 프로필을 그린다.
     */
    public MemberPublicProfileDto publicProfile(long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        MemberProfile profile = memberProfileRepository.findByMember(member)
                .orElseGet(() -> new MemberProfile(member));

        // 달성한 성취는 타입을 가리지 않고 전부 싣는다 - 기획서 3.7 이 "성취에는 공개 여부 필드를 두지 않는다,
        // 모든 Goal 이 기본적으로 전체 공개" 라고 못박았다. 파티장이 이력을 빠짐없이 보는 게 목적이다(2.5).
        // 화면이 CONTEST 를 '수상', PROJECT 를 '참여한 프로젝트' 로 나눠 그릴 뿐 서버가 골라내지 않는다.
        return new MemberPublicProfileDto(
                profile,
                memberSummaryService.summary(member),
                goalService.getMyGoals(member, GoalStatus.ACHIEVED, null, null, null, null)
        );
    }

    /**
     * 공개 프로필의 '참여한 프로젝트'.
     *
     * 프로필 본문과 나눠 둔 것은 성격이 달라서다 - 이쪽은 전시글 목록이고, 전시가 없는 회원이 대부분이다.
     * 요약의 '자동기록' 건수와 같은 조건(파티 확정 명단 + 게시됨)이라 카드 수와 그 숫자가 어긋나지 않는다.
     */
    public List<MemberShowcaseDto> publicShowcases(long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("404-1", "회원을 찾을 수 없습니다."));

        return partyShowcaseRepository.findPublishedByAssembledMember(member)
                .stream()
                .map(MemberShowcaseDto::new)
                .toList();
    }

    @Transactional
    public MemberProfileDto modifyProfile(
            Member actor, String nickname, String webpage, String profileImageUrl,
            String bio,
            PositionType position, List<String> techStacks,
            List<CareerCommand> careers, List<LinkCommand> links) {

        MemberProfile profile = getOrCreateProfile(actor);

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

    // 조회(GET /me)와 자동저장(PATCH /me)이 동시에 들어와 각자 INSERT 하면 member 유니크 제약에 걸린다.
    // 없을 때만 회원 행을 잠가 최초 생성을 한 줄로 세운다 - 있으면 잠그지 않는다.
    private MemberProfile getOrCreateProfile(Member actor) {
        return memberProfileRepository.findByMember(actor)
                .orElseGet(() -> {
                    memberRepository.findByIdForUpdate(actor.getId());

                    return memberProfileRepository.findByMember(actor)
                            .orElseGet(() -> memberProfileRepository.save(new MemberProfile(actor)));
                });
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
