package com.back.domain.party.github.controller;

import com.back.domain.party.github.service.GithubInstallationInventoryService;
import com.back.domain.party.github.service.PartyGithubConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/github-app")
@RequiredArgsConstructor
public class ApiV1GithubAppSetupController {
    private final PartyGithubConnectionService connectionService;
    private final GithubInstallationInventoryService inventoryService;
    @Value("${custom.frontend.base-url:/}") private String frontendBaseUrl;

    @GetMapping("/setup")
    public ResponseEntity<Void> setup(@RequestParam(required = false) String state,
                                      @RequestParam(name = "installation_id") long installationId) {
        if (state == null || state.isBlank()) {
            inventoryService.syncInstallation(installationId);
            return ResponseEntity.status(HttpStatus.FOUND).location(frontendUri("github-app/installed")).build();
        }
        PartyGithubConnectionService.InstallCompletion completion = connectionService.completeInstall(state, installationId);
        String path = completion.redirectPath() == null ? "/parties/" + completion.partyId() : completion.redirectPath();
        return ResponseEntity.status(HttpStatus.FOUND).location(frontendUri(path.substring(1))).build();
    }

    private URI frontendUri(String path) {
        String base = frontendBaseUrl.endsWith("/") ? frontendBaseUrl : frontendBaseUrl + "/";
        return URI.create(base).resolve(path);
    }
}
