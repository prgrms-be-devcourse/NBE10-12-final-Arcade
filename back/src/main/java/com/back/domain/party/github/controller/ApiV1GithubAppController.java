package com.back.domain.party.github.controller;

import com.back.domain.party.github.dtos.GithubAppInstallUrlDto;
import com.back.domain.party.github.dtos.GithubConnectableRepositoryDto;
import com.back.domain.party.github.service.PartyGithubBindingService;
import com.back.domain.party.github.service.PartyGithubConnectionService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/github-app")
@RequiredArgsConstructor
public class ApiV1GithubAppController {
    private final PartyGithubConnectionService connectionService;
    private final PartyGithubBindingService bindingService;
    private final Rq rq;

    @PostMapping("/install")
    public RsData<GithubAppInstallUrlDto> install() {
        return new RsData<>("200-1", "GitHub App 설치 URL 생성 성공", connectionService.beginGlobalInstall(rq.getActorFromDb()));
    }

    @GetMapping("/repositories")
    public RsData<List<GithubConnectableRepositoryDto>> repositories() {
        return new RsData<>("200-1", "접근 가능한 GitHub App 저장소 조회 성공",
                bindingService.getConnectableRepositories(rq.getActorFromDb()));
    }
}
