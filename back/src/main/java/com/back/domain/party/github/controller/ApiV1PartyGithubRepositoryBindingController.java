package com.back.domain.party.github.controller;

import com.back.domain.party.github.dtos.PartyGithubBindingCreateRq;
import com.back.domain.party.github.dtos.PartyGithubBindingDto;
import com.back.domain.party.github.service.PartyGithubBindingService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parties/{partyId}/github-repository-binding")
@RequiredArgsConstructor
public class ApiV1PartyGithubRepositoryBindingController {
    private final PartyGithubBindingService bindingService;
    private final Rq rq;

    @PostMapping
    public RsData<PartyGithubBindingDto> connect(@PathVariable long partyId, @Valid @RequestBody PartyGithubBindingCreateRq body) {
        return new RsData<>("201-1", "GitHub PR 연동 성공", bindingService.connect(partyId, body.installationRepositoryId(), rq.getActorFromDb()));
    }

    @DeleteMapping
    public RsData<Void> disconnect(@PathVariable long partyId) {
        bindingService.disconnect(partyId, rq.getActorFromDb());
        return new RsData<>("200-1", "GitHub PR 연동 해제 성공");
    }
}
