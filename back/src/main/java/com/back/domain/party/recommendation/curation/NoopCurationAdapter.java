package com.back.domain.party.recommendation.curation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

// custom.curation.enabled=false(기본값)일 때 쓰이는 기본 큐레이션
// 키워드 매칭 순서를 그대로 rank로 쓰고 reason은 비워둔다. 폴백, 테스트용
@Component
@ConditionalOnProperty(name = "custom.curation.enabled", havingValue = "false", matchIfMissing = true)
public class NoopCurationAdapter implements PartyCurationPort {

    @Override
    public List<CurationResult> curate(MemberCurationContext memberContext, List<PartyCandidate> candidates) {
        return PartyCurationPort.fallbackOrder(candidates);
    }
}
