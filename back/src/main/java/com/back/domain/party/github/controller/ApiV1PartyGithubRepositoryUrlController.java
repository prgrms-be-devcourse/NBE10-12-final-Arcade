package com.back.domain.party.github.controller;

import com.back.domain.party.github.dtos.PartyGithubRepositoryUrlRq;
import com.back.domain.party.github.service.PartyGithubRepositoryUrlService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parties/{partyId}/github-repository-url")
@RequiredArgsConstructor
public class ApiV1PartyGithubRepositoryUrlController {
    private final PartyGithubRepositoryUrlService urlService;
    private final Rq rq;

    @PatchMapping
    public RsData<String> update(@PathVariable long partyId, @Valid @RequestBody PartyGithubRepositoryUrlRq body) {
        return new RsData<>("200-1", "GitHub 저장소 URL 저장 성공", urlService.update(partyId, body.githubRepoUrl(), rq.getActorFromDb()));
    }
}
