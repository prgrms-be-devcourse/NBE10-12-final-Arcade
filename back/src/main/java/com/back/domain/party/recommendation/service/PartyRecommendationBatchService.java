package com.back.domain.party.recommendation.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.entity.MemberProfileTechStack;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.recommendation.curation.CurationResult;
import com.back.domain.party.recommendation.curation.MemberCurationContext;
import com.back.domain.party.recommendation.curation.PartyCandidate;
import com.back.domain.party.recommendation.curation.PartyCurationPort;
import com.back.domain.party.recommendation.entity.PartyRecommendation;
import com.back.domain.party.recommendation.repository.PartyRecommendationRepository;
import com.back.domain.search.search.entity.SearchLog;
import com.back.domain.search.search.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyRecommendationBatchService {

    private static final int RECOMMENDATION_SIZE = 10;
    private static final int MEMBER_CHUNK_SIZE = 500;
    private static final int SEARCH_LOG_WINDOW_DAYS = 30;
    private static final int SEARCH_LOG_LIMIT = 6;

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PartyRecommendationCandidateService candidateService;
    private final PartyRecommendationRepository partyRecommendationRepository;
    private final PartyRepository partyRepository;
    private final SearchLogRepository searchLogRepository;
    private final PartyCurationPort partyCurationPort;

    @Value("${custom.curation.candidate-limit:15}")
    private int candidateLimit;

    @Autowired
    @Lazy
    private PartyRecommendationBatchService self;

    @Scheduled(cron = "${custom.curation.batch-cron:0 0 3 * * *}")
    public void computeAll() {
        Pageable pageable = PageRequest.of(0, MEMBER_CHUNK_SIZE);
        Slice<Long> page;
        do {
            page = memberProfileRepository.findAllMemberIds(pageable);

            for (Long memberId : page.getContent()) {
                try {
                    self.computeForMember(memberId);
                } catch (Exception e) {
                    log.warn("회원 {} 추천 계산 실패", memberId, e);
                }
            }

            pageable = pageable.next();
        } while (page.hasNext());
    }

    @Transactional
    public void computeForMember(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        computeForMember(member);
    }

    @Transactional
    public void computeForMember(Member member) {
        List<Long> candidatePartyIds = candidateService.selectCandidatePartyIds(member, candidateLimit);

        partyRecommendationRepository.deleteByMemberId(member.getId());

        List<PartyRecommendation> recommendations = curate(member, candidatePartyIds).stream()
                .limit(RECOMMENDATION_SIZE)
                .map(result -> new PartyRecommendation(member.getId(), result.partyId(), result.rank(), result.reason()))
                .toList();

        partyRecommendationRepository.saveAll(recommendations);
    }

    private List<CurationResult> curate(Member member, List<Long> candidatePartyIds) {
        if (candidatePartyIds.isEmpty()) {
            return List.of();
        }

        List<PartyCandidate> candidates = partyRepository.findAllById(candidatePartyIds).stream()
                .map(this::toPartyCandidate)
                .toList();

        MemberCurationContext context = buildMemberContext(member);

        return partyCurationPort.curate(context, candidates);
    }

    private PartyCandidate toPartyCandidate(Party party) {
        return new PartyCandidate(party.getId(), party.getTitle(), party.getDescription(), party.getTopicType(), party.getPartyTag());
    }

    private MemberCurationContext buildMemberContext(Member member) {
        MemberProfile profile = memberProfileRepository.findWithTechStacksByMember(member).orElse(null);
        List<String> techStacks = profile == null ? List.of() : profile.getTechStacks().stream()
                .map(MemberProfileTechStack::getTechStack)
                .toList();

        List<SearchLog> recentLogs = searchLogRepository.findByMemberAndCreateDateAfterOrderByCreateDateDesc(
                member, LocalDateTime.now().minusDays(SEARCH_LOG_WINDOW_DAYS), PageRequest.of(0, SEARCH_LOG_LIMIT)
        );
        List<String> recentKeywords = recentLogs.stream().map(SearchLog::getKeyword).toList();

        return new MemberCurationContext(profile == null ? null : profile.getPosition(), techStacks, recentKeywords);
    }
}
