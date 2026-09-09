package com.back.domain.party.party.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PartyServiceSearchIndexingTest {

    @Autowired
    private PartyService partyService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private PartySearchKeywordRepository partySearchKeywordRepository;

    @Test
    void creatingAPartyIndexesItForSearch() {
        Member owner = saveOwner("index-owner1@test.com");

        PartyDto created = partyService.create(
                owner, "파티명", "백엔드 스터디원 모집", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7),
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3))
        );

        Optional<PartySearchKeyword> indexed = partySearchKeywordRepository.findByParty_Id(created.id());

        assertThat(indexed).isPresent();
    }

    @Test
    void updatingTitleReindexes() {
        Member owner = saveOwner("index-owner2@test.com");
        PartyDto created = partyService.create(
                owner, "파티명", "백엔드 스터디원 모집", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7),
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3))
        );

        partyService.update(
                created.id(), owner, "파티명", "프론트엔드 스터디원 모집", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7),
                null
        );

        Optional<PartySearchKeyword> reindexed = partySearchKeywordRepository.findByParty_Id(created.id());

        assertThat(reindexed).hasValueSatisfying(keyword -> assertThat(keyword.getKeywords()).contains("프론트엔드"));
    }

    @Test
    void deletingPartyRemovesSearchIndex() {
        Member owner = saveOwner("index-owner3@test.com");
        PartyDto created = partyService.create(
                owner, "파티명", "백엔드 스터디원 모집", null, null, "외부 대회", "https://example.com",
                TopicType.STUDY, PartyTag.WEB, null, LocalDateTime.now().plusDays(7),
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3))
        );

        partyService.deletePartyAndInteractions(created.id(), owner);

        assertThat(partySearchKeywordRepository.findByParty_Id(created.id())).isEmpty();
    }

    private Member saveOwner(String email) {
        Member owner = memberRepository.save(new Member(email, "pw", "owner", null));
        memberProfileRepository.save(
                new MemberProfile(owner, null, null, PositionType.BACK, List.of()));

        return owner;
    }
}
