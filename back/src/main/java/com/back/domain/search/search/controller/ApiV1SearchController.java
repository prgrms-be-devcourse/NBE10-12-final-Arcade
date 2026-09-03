package com.back.domain.search.search.controller;

import com.back.domain.search.search.dtos.PartySearchResultDto;
import com.back.domain.search.search.service.party.PartySearchService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/parties")
@Validated
@Tag(name = "ApiV1SearchController", description = "파티 검색 컨트롤러")
public class ApiV1SearchController {

    private final PartySearchService partySearchService;
    private final Rq rq;

    @GetMapping("/search")
    @Operation(
            summary = "유사 파티 키워드 검색",
            description = """
                    형태소 분석으로 추출한 키워드 기반으로 제목이 달라도 주제가 유사한 파티를 함께 노출한다.

                    예외
                    - 400-1 : q 누락 등 검증 실패
                    - 400-4 : q가 너무 짧아 키워드 추출 불가
                    """
    )
    public RsData<PartySearchResultDto> search(
            @Parameter(description = "검색어")
            @RequestParam(required = false) @NotBlank @Size(max = 25) String q,
            @Parameter(description = "0부터 시작하는 페이지 번호")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PartySearchResultDto result = partySearchService.search(
                rq.getActorFromDb(),
                q,
                PageRequest.of(page, size)
        );

        return new RsData<>("200-1", "유사 파티 검색 성공", result);
    }
}
