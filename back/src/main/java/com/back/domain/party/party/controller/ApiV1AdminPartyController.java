package com.back.domain.party.party.controller;

import com.back.domain.party.party.dtos.PartyListItemDto;
import com.back.domain.party.party.service.PartyService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 인가는 SecurityConfig의 "/api/*/adm/**" -> hasRole("ADMIN") 매칭에 이미 걸려 있어
// 이 컨트롤러 자체에는 별도 권한 애노테이션이 필요 없다.
@RestController
@RequestMapping("/api/v1/adm/parties")
@RequiredArgsConstructor
public class ApiV1AdminPartyController {

    private final PartyService partyService;

    @GetMapping
    public RsData<Page<PartyListItemDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PartyListItemDto> parties = partyService.getListForAdmin(
                keyword,
                hidden,
                PageRequest.of(page, size)
        );

        return new RsData<>("200-1", "관리자 파티 목록 조회 성공", parties);
    }

    @PatchMapping("/{partyId}/hidden")
    public RsData<Void> hide(@PathVariable long partyId) {
        partyService.hide(partyId);
        return new RsData<>("200-1", "파티 숨김 처리 성공", null);
    }

    @DeleteMapping("/{partyId}/hidden")
    public RsData<Void> unhide(@PathVariable long partyId) {
        partyService.unhide(partyId);
        return new RsData<>("200-1", "파티 숨김 해제 성공", null);
    }

    @DeleteMapping("/{partyId}")
    public RsData<Void> delete(@PathVariable long partyId) {
        partyService.deleteAsAdmin(partyId);
        return new RsData<>("204-1", "파티 강제 삭제 성공", null);
    }
}
