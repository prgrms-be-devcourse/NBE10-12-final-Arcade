package com.back.domain.party.showcase.comment.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.assemble.repository.PartyAssembleToMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.showcase.comment.dtos.ShowcaseCommentDto;
import com.back.domain.party.showcase.comment.entity.ShowcaseComment;
import com.back.domain.party.showcase.comment.event.ShowcaseCommentCreatedEvent;
import com.back.domain.party.showcase.comment.repository.ShowcaseCommentRepository;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowcaseCommentService {

    private final PartyRepository partyRepository;
    private final PartyShowcaseRepository partyShowcaseRepository;
    private final ShowcaseCommentRepository showcaseCommentRepository;
    private final PartyAssembleToMemberRepository partyAssembleToMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<ShowcaseCommentDto> getComments(long partyId) {
        PartyShowcase showcase = findPublishedShowcaseOrThrow(partyId);
        List<ShowcaseComment> comments = showcaseCommentRepository.findAllByShowcaseOrderByIdAsc(showcase);
        Set<Long> partyMemberIds = getPartyMemberIds(showcase.getParty());

        Map<Long, List<ShowcaseComment>> repliesByParentId = comments.stream()
                .filter(ShowcaseComment::isReply)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return comments.stream()
                .filter(c -> !c.isReply())
                .map(c -> toDto(c, repliesByParentId.getOrDefault(c.getId(), List.of()), partyMemberIds))
                .toList();
    }

    @Transactional
    public ShowcaseCommentDto write(long partyId, Member actor, Long parentId, String content) {
        PartyShowcase showcase = findPublishedShowcaseOrThrow(partyId);
        ShowcaseComment parent = resolveParent(showcase, parentId);

        ShowcaseComment comment = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, actor, parent, content));

        eventPublisher.publishEvent(new ShowcaseCommentCreatedEvent(partyId, actor.getId()));

        Set<Long> partyMemberIds = getPartyMemberIds(showcase.getParty());
        return toDto(comment, List.of(), partyMemberIds);
    }

    @Transactional
    public void edit(long partyId, long commentId, Member actor, String content) {
        ShowcaseComment comment = findCommentOrThrow(partyId, commentId);
        if (!comment.isAuthor(actor)) {
            throw new ServiceException("403-1", "작성자만 수정할 수 있습니다.");
        }
        comment.edit(content);
    }

    @Transactional
    public void delete(long partyId, long commentId, Member actor) {
        ShowcaseComment comment = findCommentOrThrow(partyId, commentId);
        if (!comment.isAuthor(actor) && !actor.isAdmin()) {
            throw new ServiceException("403-1", "작성자 또는 관리자만 삭제할 수 있습니다.");
        }
        comment.softDelete();
    }

    private ShowcaseComment resolveParent(PartyShowcase showcase, Long parentId) {
        if (parentId == null) {
            return null;
        }
        ShowcaseComment parent = showcaseCommentRepository.findByIdAndShowcase(parentId, showcase)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 댓글입니다."));
        if (parent.isReply()) {
            throw new ServiceException("400-1", "대댓글에는 답글을 달 수 없습니다.");
        }
        return parent;
    }

    private ShowcaseComment findCommentOrThrow(long partyId, long commentId) {
        PartyShowcase showcase = findShowcaseOrThrow(partyId);
        return showcaseCommentRepository.findByIdAndShowcase(commentId, showcase)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 댓글입니다."));
    }

    private PartyShowcase findPublishedShowcaseOrThrow(long partyId) {
        PartyShowcase showcase = findShowcaseOrThrow(partyId);
        if (!showcase.isPublished()) {
            throw new ServiceException("404-1", "게시되지 않은 전시입니다.");
        }
        return showcase;
    }

    private PartyShowcase findShowcaseOrThrow(long partyId) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
        return partyShowcaseRepository.findByParty(party)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 전시입니다."));
    }

    private Set<Long> getPartyMemberIds(Party party) {
        return partyAssembleToMemberRepository.findAllByPartyAssemble_PartyOrderByIdAsc(party).stream()
                .map(atm -> atm.getMember().getId())
                .collect(Collectors.toSet());
    }

    private ShowcaseCommentDto toDto(ShowcaseComment comment, List<ShowcaseComment> replies, Set<Long> partyMemberIds) {
        return new ShowcaseCommentDto(
                comment.getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                partyMemberIds.contains(comment.getAuthor().getId()),
                comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent(),
                comment.isDeleted(),
                comment.getCreateDate(),
                replies.stream().map(r -> toDto(r, List.of(), partyMemberIds)).toList()
        );
    }
}
