package com.back.domain.party.recommendation.service;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.entity.MemberProfileTechStack;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.search.search.entity.SearchLog;
import com.back.domain.search.search.repository.SearchLogRepository;
import com.back.domain.search.search.service.keyword.KeywordExtractionPort;
import com.back.domain.search.search.service.keyword.KeywordNormalizationPort;
import com.back.domain.search.search.service.party.PartyMatchQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyRecommendationCandidateService {

    private static final int SEARCH_LOG_WINDOW_DAYS = 30;
    private static final int SEARCH_LOG_LIMIT = 6;
    private static final int GOAL_LIMIT = 10;

    // 매칭 결과에서 본인 파티/지원 이력 파티를 뺀 뒤에도 limit을 채울 수 있도록 여유분을 두고 조회한다.
    private static final int CANDIDATE_FETCH_MULTIPLIER = 5;

    // excluded가 많아 한 페이지로 안 채워질 때 추가로 넘겨볼 최대 페이지 수 - 무한 루프 방지용 안전장치.
    private static final int MAX_CANDIDATE_PAGES = 5;

    private final MemberProfileRepository memberProfileRepository;
    private final SearchLogRepository searchLogRepository;
    private final GoalRepository goalRepository;
    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final KeywordExtractionPort keywordExtractionPort;
    private final KeywordNormalizationPort keywordNormalizationPort;
    private final PartyMatchQueryPort partyMatchQueryPort;

    /**
     * 회원의 포지션·기술스택·최근 검색어(30일 이내 최신 6개)·본인 성취 제목을 키워드로 묶어
     * 후보 파티 id를 매칭 점수순으로 뽑는다. 본인이 만들었거나 이미 지원/참여한 파티는 제외한다.
     */
    public List<Long> selectCandidatePartyIds(Member member, int limit) {
        MemberProfile profile = memberProfileRepository.findWithTechStacksByMember(member).orElse(null);
        if (profile == null) {
            return List.of();
        }

        Set<String> keywords = new LinkedHashSet<>();
        profile.getTechStacks().stream()
                .map(MemberProfileTechStack::getTechStack)
                .forEach(keywords::add);

        List<SearchLog> recentLogs = searchLogRepository.findByMemberAndCreateDateAfterOrderByCreateDateDesc(
                member, LocalDateTime.now().minusDays(SEARCH_LOG_WINDOW_DAYS), PageRequest.of(0, SEARCH_LOG_LIMIT)
        );
        recentLogs.stream()
                .map(SearchLog::getKeyword)
                .map(keywordExtractionPort::extract)
                .flatMap(List::stream)
                .forEach(keywords::add);

        goalRepository.findByOwnerOrderByCreateDateDesc(member, PageRequest.of(0, GOAL_LIMIT)).stream()
                .map(Goal::getTitle)
                .map(keywordExtractionPort::extract)
                .flatMap(List::stream)
                .forEach(keywords::add);

        if (keywords.isEmpty()) {
            return List.of();
        }

        List<String> normalized = keywordNormalizationPort.normalize(List.copyOf(keywords));

        Set<Long> excluded = new HashSet<>(partyRepository.findIdsByOwner(member));
        excluded.addAll(partyMemberRepository.findPartyIdsByMember(member));

        List<Long> candidates = new ArrayList<>();
        Page<Long> matched;
        int page = 0;
        do {
            Pageable pageable = PageRequest.of(page, limit * CANDIDATE_FETCH_MULTIPLIER);
            matched = partyMatchQueryPort.findMatchingPartyIds(normalized, null, null, profile.getPosition(), pageable);
            matched.getContent().stream()
                    .filter(partyId -> !excluded.contains(partyId))
                    .forEach(candidates::add);
            page++;
        } while (candidates.size() < limit && matched.hasNext() && page < MAX_CANDIDATE_PAGES);

        return candidates.stream().limit(limit).toList();
    }
}
