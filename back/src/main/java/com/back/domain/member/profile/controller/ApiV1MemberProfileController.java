package com.back.domain.member.profile.controller;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.dtos.CareerCommand;
import com.back.domain.member.profile.dtos.LinkCommand;
import com.back.domain.member.profile.dtos.MemberProfileDto;
import com.back.domain.member.profile.dtos.MemberPublicProfileDto;
import com.back.domain.member.profile.dtos.MemberShowcaseDto;
import com.back.domain.member.profile.dtos.MemberSummaryDto;
import com.back.domain.member.profile.dtos.ProfileImageDto;
import com.back.domain.member.profile.service.MemberProfileService;
import com.back.domain.member.profile.service.MemberSummaryService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "ApiV1MemberProfileController", description = "회원 프로필 컨트롤러")
public class ApiV1MemberProfileController {

    private final MemberProfileService memberProfileService;
    private final MemberSummaryService memberSummaryService;
    private final Rq rq;

    @GetMapping("/me")
    @Operation(
            summary = "내 프로필 조회",
            description = """
                    로그인한 회원의 프로필을 조회한다.
                    아직 프로필이 없으면 기본 프로필을 생성한 뒤 반환한다.

                    예외
                    - 401-1 : 미로그인
                    """
    )
    public RsData<MemberProfileDto> me() {
        return new RsData<>(
                "200-1",
                "내 정보 조회 성공",
                memberProfileService.me(rq.getActorFromDb())
        );
    }

    @GetMapping("/me/summary")
    @Operation(
            summary = "내 활동 요약 조회",
            description = """
                    마이페이지 '활동 스코어' 카드가 쓰는 집계값을 돌려준다.
                    프로필 조회(GET /me)와 나눠 둔 것은 그쪽이 세션 확인용이라 거의 모든 화면이 부르고,
                    수정(PATCH /me)도 같은 응답을 돌려주기 때문이다. 섞으면 프로필을 읽고 쓸 때마다 집계가 함께 돈다.

                    - completedParties : 승인된 파티원으로 속한 파티 중 COMPLETED 인 건
                    - awards           : 달성한 CONTEST 성취 건수
                    - exhibitions      : 승인된 파티원으로 속한 파티 중 전시가 게시된 건
                    - streakDays       : ACTIVITY_LOG 기준 연속 활동일. 오늘이 비어 있으면 어제까지의 연속
                    - joinedAt         : 가입 시각. 히어로의 '크루온 활동 N개월째'(기획서 2.11 총 활동 기간)를 여기서 센다.
                                         첫 활동일이 아니라 가입일이다 - 활동 로그를 정리하면 이미 보여준 기간이 줄어든다
                    - badges           : 배지 도메인이 없어 아직 빈 배열

                    예외
                    - 401-1 : 미로그인
                    """
    )
    public RsData<MemberSummaryDto> summary() {
        return new RsData<>(
                "200-1",
                "내 활동 요약 조회 성공",
                memberSummaryService.summary(rq.getActorFromDb())
        );
    }

    @GetMapping("/{memberId:\\d+}")
    @Operation(
            summary = "공개 프로필 조회",
            description = """
                    남의 프로필을 본다. 지원자 심사·전시에서 이름을 눌러 들어오는 화면이다.

                    로그인 없이 열린다 - 조회는 비인증, 쓰기·댓글만 인증이 기획서 9.x 의 규칙이고
                    파티·대회·전시·성취 조회와 같은 취급이다. 공개해선 안 되는 값은 인가가 아니라
                    응답에서 뺀다(email·githubLinked).

                    내 프로필(GET /me)과 거의 같지만 email·githubLinked 는 빠진다. 본인 화면에서만 쓰는 값이다.
                    대신 집계(GET /me/summary)와 CONTEST 성취를 한 응답에 함께 싣는다 -
                    이 경로는 프로필을 열 때만 불려, 둘을 나눠 둘 이유였던 '너무 자주 불린다' 가 없다.

                    - completedParties / awards / exhibitions : /me/summary 와 같은 집계
                    - streakDays / activityHeatmap            : 연속 활동일과 최근 8주 활동 농도
                    - achievements                            : 달성한 성취 전부. 타입을 가리지 않는다
                                                                (기획서 3.7 - 성취에 공개 여부 필드를 두지 않고 모두 전체 공개).
                                                                CONTEST 는 '수상', PROJECT 는 '참여한 프로젝트' 로 화면이 나눠 그린다
                    - joinedAt                                : 가입 시각. '크루온 활동 N개월째' 를 여기서 센다
                    - badges                                  : 배지 도메인이 없어 아직 응답에 없다

                    프로필을 한 번도 저장한 적 없는 회원도 404 가 아니라 빈 프로필로 돌려준다.
                    조회가 프로필 행을 만들지는 않는다.

                    예외
                    - 404-1 : 없는 회원
                    """
    )
    public RsData<MemberPublicProfileDto> publicProfile(@PathVariable long memberId) {
        return new RsData<>(
                "200-1",
                "공개 프로필 조회 성공",
                memberProfileService.publicProfile(memberId)
        );
    }

    @GetMapping("/{memberId:\\d+}/showcases")
    @Operation(
            summary = "공개 프로필의 참여한 프로젝트",
            description = """
                    그 회원이 참여한 파티 중 전시가 게시된 것을 최근 게시순으로 돌려준다.
                    공개 프로필과 같이 로그인 없이 열린다.

                    기준은 파티 확정 명단(PARTY_ASSEMBLE_TO_MEMBER)이다.
                    id 는 goal id 가 아니라 **partyId** 다 - 전시는 파티에 종속이라 상세 경로도 파티 기준이다.

                    예외
                    - 404-1 : 없는 회원
                    """
    )
    public RsData<List<MemberShowcaseDto>> publicShowcases(@PathVariable long memberId) {
        return new RsData<>(
                "200-1",
                "참여한 프로젝트 조회 성공",
                memberProfileService.publicShowcases(memberId)
        );
    }

    public record ModifyProfileReqBody(
            // 보내지 않으면 그대로 둔다. 다만 빈 문자열은 실수로 지우는 경우라 막는다.
            // (?U) 없이는 \S 가 ASCII 공백만 부정해 전각 공백(U+3000) 한 글자짜리 닉네임이 통과한다.
            @Pattern(regexp = "(?U).*\\S.*", message = "닉네임은 공백일 수 없습니다.")
            String nickname,
            String webpage,
            String profileImageUrl,
            String bio,
            PositionType position,
            List<@NotBlank String> techStacks,
            List<CareerCommand> careers,
            List<LinkCommand> links
    ) {
    }


    @PatchMapping("/me")
    @Operation(
            summary = "내 프로필 수정",
            description = """
                    로그인한 회원의 닉네임, 웹페이지, 프로필 이미지, 소개,
                    희망 포지션, 기술 스택, 경력, 링크를 수정한다.

                    **보낸 항목만 바뀐다.** 생략하거나 null 을 보내면 그 항목은 그대로 둔다 -
                    화면이 폼 전체를 들고 있지 않아도 되고, 다루지 않는 필드가 저장할 때마다 지워지지 않는다.
                    전부 선택이라 { "bio": "..." } 처럼 한 필드만 보내도 된다.

                    **비우려면 빈 값을 명시한다** - 문자열은 ""(webpage·bio·profileImageUrl),
                    목록은 [](techStacks·careers·links). position 은 비울 수 없다(값 하나를 골라 바꾸기만 한다).

                    position은 대표 포지션 하나이고 BACK/FRONT/UIUX/PM 중 하나다.
                    techStacks의 원소는 빈 문자열이면 400-1이다.

                    careers·links는 목록을 보내면 통째로 교체한다.
                    role(경력) 또는 label·url(링크)이 비어 있는 항목은 무시한다.

                    profileImageUrl은 직접 올린 이미지만 담는다. 조회 응답은 이 값과 githubAvatarUrl을
                    합치지 않고 그대로 내려주니, 화면이 profileImageUrl ?? githubAvatarUrl로 고르면 된다.

                    예외
                    - 400-1 : nickname 이 빈 문자열이거나 techStacks 원소가 빈 문자열
                    - 400-2 : position이 정의된 값이 아님
                    - 401-1 : 미로그인
                    - 409-1 : 이미 사용 중인 닉네임
                    """
    )
    public RsData<MemberProfileDto> modifyProfile(
            @Valid @RequestBody ModifyProfileReqBody request
    ) {
        return new RsData<>(
                "200-1",
                "내 정보 수정 성공",
                memberProfileService.modifyProfile(
                        rq.getActorFromDb(),
                        request.nickname,
                        request.webpage,
                        request.profileImageUrl,
                        request.bio,
                        request.position,
                        request.techStacks,
                        request.careers,
                        request.links
                )
        );
    }

    @PostMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "프로필 이미지 업로드",
            description = """
                    이미지를 저장하고 그 URL 을 돌려준다. 프로필에 반영되지는 않으니,
                    화면은 받은 profileImageUrl 을 수정 요청(PATCH /me)의 같은 이름 필드에 실어 보내야 한다.
                    이미지를 바꾸지 않는 수정이라면 이 요청 없이, profileImageUrl 을 빼고 PATCH 만 보내면 된다.

                    jpg, png 만 받고 5MB 까지다. 화면의 업로드 컴포넌트와 같은 기준이다.

                    예외
                    - 400-1 : 파일이 비었거나, 허용하지 않는 형식이거나, 5MB 초과
                    - 401-1 : 미로그인
                    """
    )
    public RsData<ProfileImageDto> uploadProfileImage(
            @RequestPart("file") MultipartFile file
    ) {
        return new RsData<>(
                "201-1",
                "프로필 이미지 업로드 성공",
                new ProfileImageDto(memberProfileService.uploadProfileImage(file))
        );
    }
}
