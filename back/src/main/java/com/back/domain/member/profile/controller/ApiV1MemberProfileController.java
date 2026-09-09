package com.back.domain.member.profile.controller;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.dtos.CareerCommand;
import com.back.domain.member.profile.dtos.LinkCommand;
import com.back.domain.member.profile.dtos.MemberProfileDto;
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
                    - streakDays       : 일자별 활동 기록 도메인이 없어 아직 0 고정
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
                    githubAvatarUrl을 수정 요청에 실으면 아바타 주소가 '직접 올린 것'으로 굳어
                    GitHub에서 바꿔도 반영되지 않으니 넣지 말 것.

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
