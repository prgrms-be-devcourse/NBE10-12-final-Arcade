package com.back.domain.party.party.service;

import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.repository.ContestLookupPort;
import com.back.domain.interaction.bookmark.service.BookmarkInteractionPort;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.service.LikeInteractionPort;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.dtos.PartyListItemDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartySortOption;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.event.PartySearchIndexRequestedEvent;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.search.search.service.party.PartySearchKeywordPort;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ContestLookupPort contestLookupPort;
    private final PartySearchKeywordPort partySearchKeywordPort;
    private final ApplicationEventPublisher eventPublisher;
    private final PartyMemberRepository partyMemberRepository;

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
        String contestTitle,
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

        Contest targetContest = targetContestId == null
                ? null
                : contestLookupPort.findContestById(targetContestId).orElseThrow();

        if (isMissingContestInfo(topicType, targetContest, contestTitle)) {
            throw new ServiceException("400-1", "등록된 대회가 없으면 대회명을 입력해야 합니다.");
        }

        Party party = new Party(
            owner,
            partyName,
            title,
            description,
            targetContest,
            contestTitle,
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

        Party savedParty = partyRepository.save(party);
        eventPublisher.publishEvent(new PartySearchIndexRequestedEvent(savedParty.getId()));

        return new PartyDto(savedParty);
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
            String contestTitle,
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

        Contest targetContest = targetContestId == null
                ? null
                : contestLookupPort.findContestById(targetContestId).orElseThrow();

        if (isMissingContestInfo(topicType, targetContest, contestTitle)) {
            throw new ServiceException("400-1", "등록된 대회가 없으면 대회명을 입력해야 합니다.");
        }

        String previousTitle = party.getTitle();

        party.update(
                partyName,
                title,
                description,
                targetContest,
                contestTitle,
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

        if (!previousTitle.equals(title)) {
            eventPublisher.publishEvent(new PartySearchIndexRequestedEvent(partyId));
        }

        return new PartyDto(party);
    }

    /**
     * 진행 중 파티의 GitHub App 설치 직전에 저장소 주소만 바꾼다.
     * 모집글 전체 수정은 RECRUITING 상태에서만 가능하지만, 저장소 재연동은 진행 중에도 필요하다.
     */
    @Transactional
    public PartyDto updateGithubRepository(long partyId, Member actor, String githubRepoUrl) {
        Party party = findByIdOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "파티장만 GitHub 저장소를 수정할 수 있습니다.");
        }
        if (party.getStatus() == com.back.domain.party.position.entity.PartyStatus.COMPLETED) {
            throw new ServiceException("409-1", "완료된 파티의 GitHub 저장소는 수정할 수 없습니다.");
        }

        party.updateGithubRepoUrl(githubRepoUrl.trim());
        return new PartyDto(party);
    }

    @Transactional
    public void delete(long partyId, Member actor) {
        Party party = findByIdOrThrow(partyId);

        if (!party.isOwnedBy(actor)) {
            throw new ServiceException("403-1", "본인이 만든 파티만 삭제할 수 있습니다.");
        }
        party.checkDeletable();

        // 승인된 파티원은 실제 팀 소속 관계라, 파티를 지운다고 그냥 같이 사라지면 안 된다.
        // 먼저 ApiV1PartyApplicationController.cancelApproval()로 승인을 전부 취소해야 삭제할 수 있다.
        if (partyMemberRepository.existsByPartyAndStatus(party, PartyMemberStatus.APPROVED)) {
            throw new ServiceException("409-3", "승인된 파티원이 있는 파티는 삭제할 수 없습니다. 먼저 승인을 취소해주세요.");
        }

        // PENDING/REJECTED 지원 기록은 실제 소속 관계가 아니라 단순 이력이라 파티와 함께 지운다.
        partyMemberRepository.deleteAllByParty(party);


        partyRepository.delete(party);
    }

    private Party findByIdOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }

    private boolean isMissingContestInfo(TopicType topicType, Contest targetContest, String contestTitle) {
        return topicType == TopicType.CONTEST
                && targetContest == null
                && (contestTitle == null || contestTitle.isBlank());
    }

    public Page<PartyListItemDto> getList(
            String keyword,
            PartyTag partyTag,
            PositionType positionType,
            PartySortOption sortOption,
            Pageable pageable
    ) {
        // PartyRepository의 JPQL에서 :keyword가 "is null" 비교와 "like concat" 비교
        // 두 맥락에 동시에 쓰이는데 PostgreSQL은 이 경우 파라미터 타입을 못 정해서
        // could not determine data type of parameter 에러를 던진다(H2는 통과해서 로컬에는안 드러남)
        // null을 빈 문자열로 정규화하면 파라미터가 항상 String 맥락으로만 쓰여서 이 문제가 사라지고 LIKE '%%'는 모든 행에 매치되니 키워드 없음 의미도 그대로 유지된다
        String normalizedKeyword = keyword == null ? "" : keyword;

        Page<Party> parties = switch (sortOption) {
            case DEADLINE -> partyRepository.search(
                    normalizedKeyword, partyTag, positionType,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("deadline").ascending())
            );
            case POPULAR -> partyRepository.search(
                    normalizedKeyword, partyTag, positionType,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("likeCount").descending())
            );
            case VACANCY -> partyRepository.searchOrderByVacancy(
                    normalizedKeyword, partyTag, positionType,
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
        partySearchKeywordPort.deleteKeywordParty(partyId);
        delete(partyId, actor);
        likeInteractionPort.deleteAllLikesForTarget(TargetType.PARTY, partyId);
        bookmarkInteractionPort.deleteAllBookmarksForTarget(TargetType.PARTY, partyId);
    }
}
