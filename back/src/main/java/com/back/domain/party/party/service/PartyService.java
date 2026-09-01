package com.back.domain.party.party.service;

import com.back.domain.interaction.bookmark.service.BookmarkInteractionPort;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.service.LikeInteractionPort;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.dtos.PartyListItemDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartySortOption;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.back.domain.party.party.entity.PartySortOption.DEADLINE;
import static com.back.domain.party.party.entity.PartySortOption.VACANCY;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyService {

    private final PartyRepository partyRepository;
    private final LikeInteractionPort likeInteractionPort;
    private final BookmarkInteractionPort bookmarkInteractionPort;

    public record PositionCreateSpec(
        PositionType type,
        int capacity
    ) { }

    @Transactional
    public PartyDto create(
        Member owner,
        String partyName,
        String title,
        String description,
        Long targetContestId,
        String contestName,
        String contestLinkUrl,
        TopicType topicType,
        PartyTag partyTag,
        String githubRepoUrl,
        int checklistRequiredApprovals,
        LocalDateTime deadline,
        List<PositionCreateSpec> positionSpecs
    ) {
        if (positionSpecs == null || positionSpecs.isEmpty()) {
            throw new ServiceException("400-4", "포지션 정원은 1명 이상이어야 합니다.");
        }

        positionSpecs.forEach(spec -> {
            if (spec.capacity() <= 0) {
                throw new ServiceException("400-4", "포지션 정원은 1명 이상이어야 합니다.");
            }
        });

        if (isMissingContestInfo(topicType, targetContestId, contestName)) {
            throw new ServiceException("400-1", "등록된 대회가 없으면 대회명을 입력해야 합니다.");
        }

        Party party = new Party(
            owner,
            partyName,
            title,
            description,
            targetContestId,
            contestName,
            contestLinkUrl,
            topicType,
            partyTag,
            githubRepoUrl,
            checklistRequiredApprovals,
            deadline
        );

        positionSpecs.forEach(spec ->
            party.addPosition(new Position(spec.type(), spec.capacity()))
        );

        return new PartyDto(partyRepository.save(party));
    }

    public record PositionCapacityUpdateSpec(
            long positionId,
            int capacity
    ) { }

    @Transactional
    public PartyDto update(
            long partyId,
            Member actor,
            String partyName,
            String title,
            String description,
            Long targetContestId,
            String contestName,
            String contestLinkUrl,
            TopicType topicType,
            PartyTag partyTag,
            String githubRepoUrl,
            LocalDateTime deadline,
            List<PositionCapacityUpdateSpec> positionCapacityUpdates
    ) {
        Party party = findByIdOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "본인이 만든 파티만 수정할 수 있습니다.");
        }
        party.checkModifiable();

        if (isMissingContestInfo(topicType, targetContestId, contestName)) {
            throw new ServiceException("400-1", "등록된 대회가 없으면 대회명을 입력해야 합니다.");
        }

        party.update(
                partyName,
                title,
                description,
                targetContestId,
                contestName,
                contestLinkUrl,
                topicType,
                partyTag,
                githubRepoUrl,
                deadline
        );

        if (positionCapacityUpdates != null) {
            positionCapacityUpdates.forEach(spec -> {
                if (spec.capacity() <= 0) {
                throw new ServiceException("400-4", "포지션 정원은 1명 이상이어야 합니다.");
            }
            party.findPosition(spec.positionId()).changeCapacity(spec.capacity());
        });
        }

        return new PartyDto(party);
    }

    @Transactional
    public void delete(long partyId, Member actor) {
        Party party = findByIdOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "본인이 만든 파티만 삭제할 수 있습니다.");
        }
        party.checkDeletable();

        partyRepository.delete(party);
    }

    private Party findByIdOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }

    private boolean isMissingContestInfo(TopicType topicType, Long targetContestId, String contestName) {
        return topicType == TopicType.CONTEST
                && targetContestId == null
                && (contestName == null || contestName.isBlank());
    }

    public Page<PartyListItemDto> getList(
            String keyword,
            PartyTag partyTag,
            PositionType positionType,
            PartySortOption sortOption,
            Pageable pageable
    ) {
        Page<Party> parties = switch (sortOption) {
            case DEADLINE -> partyRepository.search(
                    keyword, partyTag, positionType,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("deadline").ascending())
            );
            case POPULAR -> partyRepository.search(
                    keyword, partyTag, positionType,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("likeCount").descending())
            );
            // 빈자리 합산은 집계값이라 Sort로 표현이 안 되므로, 정렬이 쿼리 안에 이미 박혀있는
            // searchOrderByVacancy를 쓰고 Pageable은 반드시 unsorted로 넘긴다
            case VACANCY -> partyRepository.searchOrderByVacancy(
                    keyword, partyTag, positionType,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
            );
        };

        return parties.map(PartyListItemDto::new);
    }

    @Transactional
    public PartyDto getDetail(long partyId) {
        Party party = findByIdOrThrow(partyId);
        party.increaseViewCount();
        return new PartyDto(party);
    }

    // delete()만 부르면 좋아요/북마크 삭제가 별도 트랜잭션으로 빠져 원자성이 깨질 수 있어서
    @Transactional
    public void deletePartyAndInteractions(long partyId, Member actor) {
        delete(partyId, actor);
        likeInteractionPort.deleteAllLikesForTarget(TargetType.PARTY, partyId);
        bookmarkInteractionPort.deleteAllBookmarksForTarget(TargetType.PARTY, partyId);
    }
}
