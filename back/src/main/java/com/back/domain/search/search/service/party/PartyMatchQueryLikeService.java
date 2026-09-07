package com.back.domain.search.search.service.party;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.position.entity.Position;
import com.back.domain.search.search.entity.party.PartySearchKeyword;
import com.back.domain.search.search.repository.party.PartySearchKeywordRepository;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Profile("!prod")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyMatchQueryLikeService implements PartyMatchQueryPort {

    private final PartySearchKeywordRepository partySearchKeywordRepository;

    @Override
    public Page<Long> findMatchingPartyIds(
            List<String> keywords,
            String partyTag,
            String topicType,
            String positionType,
            Pageable pageable
    ) {
        Specification<PartySearchKeyword> spec = matchesAnyKeyword(keywords)
                .and(isRecruiting())
                .and(hasPartyTag(partyTag))
                .and(hasTopicType(topicType))
                .and(hasPositionType(positionType))
                .and(orderByPartyIdDesc());

        return partySearchKeywordRepository.findAll(spec, pageable)
                .map(psk -> psk.getParty().getId());
    }

    private static final char LIKE_ESCAPE_CHAR = '\\';

    private Specification<PartySearchKeyword> matchesAnyKeyword(List<String> keywords) {
        return (root, query, cb) -> keywords.stream()
                .map(keyword -> cb.like(root.get("keywords"), "%" + escapeLike(keyword) + "%", LIKE_ESCAPE_CHAR))
                .reduce(cb::or)
                .orElseGet(cb::disjunction);
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private Specification<PartySearchKeyword> isRecruiting() {
        return (root, query, cb) -> cb.equal(root.get("party").get("status"), PartyStatus.RECRUITING);
    }

    private Specification<PartySearchKeyword> hasPartyTag(String partyTag) {
        if (partyTag == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("party").get("partyTag"), PartyTag.valueOf(partyTag));
    }

    private Specification<PartySearchKeyword> hasTopicType(String topicType) {
        if (topicType == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("party").get("topicType"), TopicType.valueOf(topicType));
    }

    private Specification<PartySearchKeyword> hasPositionType(String positionType) {
        if (positionType == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var positionRoot = subquery.from(Position.class);
            subquery.select(cb.literal(1L))
                    .where(
                            cb.equal(positionRoot.get("party"), root.get("party")),
                            cb.equal(positionRoot.get("type"), PositionType.valueOf(positionType))
                    );
            return cb.exists(subquery);
        };
    }

    private Specification<PartySearchKeyword> orderByPartyIdDesc() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(cb.desc(root.get("party").get("id")));
            }
            return cb.conjunction();
        };
    }
}
