package com.back.domain.party.party.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PartyServiceSearchIndexEventTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    private <T> T awaitUntil(Supplier<T> supplier, Predicate<T> condition) throws InterruptedException {
        Duration timeout = Duration.ofSeconds(5);
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        T value = supplier.get();
        while (!condition.test(value) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            value = supplier.get();
        }
        return value;
    }

    @Test
    void creatingAPartyIndexesItForSearchAfterCommit() throws InterruptedException {
        Member owner = memberRepository.save(new Member("index-event-owner1@test.com", "pw", "owner", null));

        PartyDto created = partyService.create(
                owner, "파티명", "백엔드 스터디원 모집", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7),
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3))
        );

        Optional<PartySearchKeyword> indexed = awaitUntil(
                () -> partySearchKeywordRepository.findByParty_Id(created.id()),
                Optional::isPresent
        );

        assertThat(indexed).isPresent();
    }

    @Test
    void updatingTitleReindexesAfterCommit() throws InterruptedException {
        Member owner = memberRepository.save(new Member("index-event-owner2@test.com", "pw", "owner", null));
        PartyDto created = partyService.create(
                owner, "파티명", "백엔드 스터디원 모집", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7),
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3))
        );
        awaitUntil(() -> partySearchKeywordRepository.findByParty_Id(created.id()), Optional::isPresent);

        partyService.update(
                created.id(), owner, "파티명", "프론트엔드 스터디원 모집", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7),
                null
        );

        Optional<PartySearchKeyword> reindexed = awaitUntil(
                () -> partySearchKeywordRepository.findByParty_Id(created.id()),
                opt -> opt.isPresent() && opt.get().getKeywords().contains("프론트엔드")
        );

        assertThat(reindexed).hasValueSatisfying(keyword -> assertThat(keyword.getKeywords()).contains("프론트엔드"));
    }

    @Test
    void deletingPartyRemovesSearchIndexAfterCommit() throws InterruptedException {
        Member owner = memberRepository.save(new Member("index-event-owner3@test.com", "pw", "owner", null));
        PartyDto created = partyService.create(
                owner, "파티명", "백엔드 스터디원 모집", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, 0, LocalDateTime.now().plusDays(7),
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3))
        );
        awaitUntil(() -> partySearchKeywordRepository.findByParty_Id(created.id()), Optional::isPresent);

        partyService.deletePartyAndInteractions(created.id(), owner);

        assertThat(partySearchKeywordRepository.findByParty_Id(created.id())).isEmpty();
    }
}
