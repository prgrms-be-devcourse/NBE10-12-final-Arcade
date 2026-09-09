package com.back.domain.party.partyPr.controller;

import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.dtos.PartyPrByMemberDto;
import com.back.domain.party.partyPr.service.PartyPrQueryService;
import com.back.domain.party.github.service.PartyGithubConnectionService;
import com.back.domain.party.github.dtos.PartyGithubConnectionDto;
import com.back.domain.party.github.dtos.GithubAppInstallUrlDto;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1PartyPrController {
    private final PartyPrQueryService partyPrQueryService;
    private final PartyGithubConnectionService githubConnectionService;
    private final Rq rq;

    @GetMapping("/parties/{partyId}/github-connection")
    public RsData<PartyGithubConnectionDto> getGithubConnection(
            @PathVariable long partyId) {
        return new RsData<>("200-1", "GitHub 연결 상태 조회 성공", githubConnectionService.getStatus(partyId, rq.getActorFromDb()));
    }

    @PostMapping("/parties/{partyId}/github-app/install")
    @Deprecated(forRemoval = false)
    public RsData<GithubAppInstallUrlDto> installGithubApp(
            @PathVariable long partyId,
            @RequestParam(required = false) String redirectUrl
    ) {
        return new RsData<>("200-1", "GitHub App 설치 URL 생성 성공", githubConnectionService.beginInstall(partyId, rq.getActorFromDb(), redirectUrl));
    }


    @GetMapping("/parties/{partyId}/pull-requests")
    public RsData<List<PartyPrDto>> getPullRequests(
            @PathVariable long partyId) {
        return new RsData<>(
                "200-1",
                "PR 목록 조회 성공",
                partyPrQueryService.getByPartyId(partyId, rq.getActorFromDb())
        );
    }

    @GetMapping("/parties/{partyId}/pull-requests/grouped-by-member")
    public RsData<List<PartyPrByMemberDto>> getPullRequestsGroupedByMember(
            @PathVariable long partyId) {
        return new RsData<>(
                "200-1",
                "담당자별 PR 목록 조회 성공",
                partyPrQueryService.getByPartyIdGroupedByMember(partyId, rq.getActorFromDb())
        );
    }

    @GetMapping("/parties/{partyId}/pull-requests/members/me")
    public RsData<PartyPrByMemberDto> getMyPullRequestsInParty(
            @PathVariable long partyId) {
        var actor = rq.getActor();
        return new RsData<>(
                "200-1",
                "파티 내 PR 목록 조회 성공",
                partyPrQueryService.getByPartyIdAndMemberId(partyId, actor.getId(), actor)
        );
    }

    @GetMapping("/parties/{partyId}/pull-requests/members/{memberId:\\d+}")
    public RsData<PartyPrByMemberDto> getPullRequestsByMember(
            @PathVariable long partyId,
            @PathVariable long memberId
    ) {
        return new RsData<>(
                "200-1",
                "파티원 PR 목록 조회 성공",
                partyPrQueryService.getByPartyIdAndMemberId(partyId, memberId, rq.getActorFromDb())
        );
    }

    @GetMapping("/pull-requests/me")
    public RsData<List<PartyPrDto>> getMyPullRequests() {
        return new RsData<>("200-1", "내 GitHub PR 목록 조회 성공",
                partyPrQueryService.getMyPullRequests(rq.getActorFromDb()));
    }

}
