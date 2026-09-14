package com.back.domain.party.recommendation.curation;

import java.util.ArrayList;
import java.util.List;

public interface PartyCurationPort {

    List<CurationResult> curate(MemberCurationContext memberContext, List<PartyCandidate> candidates);

    /**
     * 큐레이션을 쓸 수 없을 때(비활성화·API 장애·쿼터초과) 공통으로 쓰는 폴백.
     * 후보 순서 그대로 rank만 매기고 reason은 비워둔다. NoopCurationAdapter와
     * GeminiCurationAdapter의 실패 처리가 이 메서드 하나를 공유해서 동작을 일치시킨다.
     */
    static List<CurationResult> fallbackOrder(List<PartyCandidate> candidates) {
        List<CurationResult> results = new ArrayList<>();
        int rank = 1;
        for (PartyCandidate candidate : candidates) {
            results.add(new CurationResult(candidate.partyId(), rank++, null));
        }
        return results;
    }
}
