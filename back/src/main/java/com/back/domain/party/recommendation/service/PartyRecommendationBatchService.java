package com.back.domain.party.recommendation.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.recommendation.entity.PartyRecommendation;
import com.back.domain.party.recommendation.repository.PartyRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyRecommendationBatchService {

    private static final int RECOMMENDATION_SIZE = 10;
    private static final int MEMBER_CHUNK_SIZE = 500;

    private final MemberProfileRepository memberProfileRepository;
    private final PartyRecommendationCandidateService candidateService;
    private final PartyRecommendationRepository partyRecommendationRepository;

    @Autowired
    @Lazy
    private PartyRecommendationBatchService self;

    // 매일 새벽 3시 - 새벽대 트래픽이 가장 낮은 시간대. FeaturedRankingBatchService(자정)와 안 겹치게 띄운다.
    @Scheduled(cron = "0 0 3 * * *")
    public void computeAll() {
        Pageable pageable = PageRequest.of(0, MEMBER_CHUNK_SIZE);
        Page<MemberProfile> page;
        do {
            page = memberProfileRepository.findAll(pageable);

            for (MemberProfile profile : page.getContent()) {
                Member member = profile.getMember();
                try {
                    self.computeForMember(member);
                } catch (Exception e) {
                    log.warn("회원 {} 추천 계산 실패", member.getId(), e);
                }
            }

            pageable = pageable.next();
        } while (page.hasNext());
    }

    @Transactional
    public void computeForMember(Member member) {
        List<Long> candidatePartyIds = candidateService.selectCandidatePartyIds(member, RECOMMENDATION_SIZE);

        partyRecommendationRepository.deleteByMemberId(member.getId());

        List<PartyRecommendation> recommendations = new ArrayList<>();
        int rank = 1;
        for (Long partyId : candidatePartyIds) {
            recommendations.add(new PartyRecommendation(member.getId(), partyId, rank++, null));
        }
        partyRecommendationRepository.saveAll(recommendations);
    }
}
