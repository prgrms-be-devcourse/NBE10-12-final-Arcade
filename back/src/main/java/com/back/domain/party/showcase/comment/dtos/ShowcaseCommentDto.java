package com.back.domain.party.showcase.comment.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record ShowcaseCommentDto(
        long id,
        long authorId,
        String authorName,
        boolean isPartyMember,
        String content,
        boolean deleted,
        LocalDateTime createDate,
        List<ShowcaseCommentDto> replies
) {
}
