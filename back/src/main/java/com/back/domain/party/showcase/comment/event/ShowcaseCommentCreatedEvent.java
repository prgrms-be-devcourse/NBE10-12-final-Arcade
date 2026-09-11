package com.back.domain.party.showcase.comment.event;

public record ShowcaseCommentCreatedEvent(
        long partyId,
        long authorId
) {
}
