package com.back.domain.party.partyPr.controller;

import com.back.domain.party.partyPr.dtos.PartyPrByMemberDto;
import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.partyPr.service.PartyPrQueryService;
import com.back.domain.party.partyPr.service.PartyPrSseService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1PartyPrStreamController {
    private final PartyPrQueryService queryService;
    private final PartyPrSseService sseService;
    private final Rq rq;

    @GetMapping(value = "/parties/{partyId}/pull-requests/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable long partyId, HttpServletResponse response) {
        try { return sseService.subscribe(partyId, queryService.getByPartyId(partyId, rq.getActorFromDb())); }
        catch (ServiceException exception) { return rejected(response, exception); }
    }

    @GetMapping(value = "/parties/{partyId}/pull-requests/grouped-by-member/stream", produces = "text/event-stream")
    public SseEmitter grouped(@PathVariable long partyId, HttpServletResponse response) {
        try { return sseService.subscribeGrouped(partyId, queryService.getByPartyIdGroupedByMember(partyId, rq.getActorFromDb())); }
        catch (ServiceException exception) { return rejected(response, exception); }
    }

    @GetMapping(value = "/parties/{partyId}/pull-requests/members/me/stream", produces = "text/event-stream")
    public SseEmitter mine(@PathVariable long partyId, HttpServletResponse response) {
        try { var actor = rq.getActor(); PartyPrByMemberDto snapshot = queryService.getByPartyIdAndMemberId(partyId, actor.getId(), actor); return sseService.subscribeMember(partyId, snapshot.githubUserId(), snapshot); }
        catch (ServiceException exception) { return rejected(response, exception); }
    }

    @GetMapping(value = "/parties/{partyId}/pull-requests/members/{memberId:\\d+}/stream", produces = "text/event-stream")
    public SseEmitter member(@PathVariable long partyId, @PathVariable long memberId, HttpServletResponse response) {
        try { PartyPrByMemberDto snapshot = queryService.getByPartyIdAndMemberId(partyId, memberId, rq.getActorFromDb()); return sseService.subscribeMember(partyId, snapshot.githubUserId(), snapshot); }
        catch (ServiceException exception) { return rejected(response, exception); }
    }

    private SseEmitter rejected(HttpServletResponse response, ServiceException exception) {
        response.setStatus(exception.getRsData().statusCode());
        SseEmitter emitter = new SseEmitter(0L); emitter.complete(); return emitter;
    }
}
