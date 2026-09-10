package com.back.global.github.controller;

import com.back.global.github.service.GithubWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
public class GithubWebhookController {
    private final GithubWebhookService service;
    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(@RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId, @RequestBody byte[] body) {
        service.receive(event, signature, deliveryId, body);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
