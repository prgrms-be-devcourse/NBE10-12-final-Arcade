package com.back.domain.party.showcase.comment.repository;

import com.back.domain.party.showcase.comment.entity.ShowcaseComment;
import com.back.domain.party.showcase.entity.PartyShowcase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShowcaseCommentRepository extends JpaRepository<ShowcaseComment, Long> {

    interface TargetCount {
        Long getShowcaseId();
        Long getCount();
    }

    @EntityGraph(attributePaths = {"author", "parent"})
    List<ShowcaseComment> findAllByShowcaseOrderByIdAsc(PartyShowcase showcase);

    Optional<ShowcaseComment> findByIdAndShowcase(long id, PartyShowcase showcase);

    @Query("""
        select c.showcase.id as showcaseId, count(c) as count
        from ShowcaseComment c
        where c.showcase.id in :showcaseIds
          and c.parent is null
          and c.deleted = false
          and c.createDate >= :from
        group by c.showcase.id
        """)
    List<TargetCount> countRootCommentsGroupedByShowcaseIdInAndCreateDateAfter(
            @Param("showcaseIds") Collection<Long> showcaseIds,
            @Param("from") LocalDateTime from
    );
}
