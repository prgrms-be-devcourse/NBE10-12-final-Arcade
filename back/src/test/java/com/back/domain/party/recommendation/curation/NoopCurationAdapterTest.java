package com.back.domain.party.recommendation.curation;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoopCurationAdapterTest {

    private final NoopCurationAdapter adapter = new NoopCurationAdapter();

    @Test
    @DisplayName("후보 순서 그대로 rank를 매기고 reason은 비운다")
    void curateKeepsOrderAndClearsReason() {
        MemberCurationContext context = new MemberCurationContext(PositionType.BACK, List.of("Java"), List.of());
        List<PartyCandidate> candidates = List.of(
                new PartyCandidate(1L, "백엔드 스터디", "설명1", TopicType.STUDY, PartyTag.WEB),
                new PartyCandidate(2L, "프론트 프로젝트", "설명2", TopicType.PROJECT, PartyTag.WEB)
        );

        List<CurationResult> results = adapter.curate(context, candidates);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).partyId()).isEqualTo(1L);
        assertThat(results.get(0).rank()).isEqualTo(1);
        assertThat(results.get(0).reason()).isNull();
        assertThat(results.get(1).partyId()).isEqualTo(2L);
        assertThat(results.get(1).rank()).isEqualTo(2);
    }
}
