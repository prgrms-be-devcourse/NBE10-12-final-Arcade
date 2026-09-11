package com.back.domain.party.showcase.comment.repository;

import com.back.domain.party.showcase.comment.entity.ShowcaseComment;
import com.back.domain.party.showcase.entity.PartyShowcase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShowcaseCommentRepository extends JpaRepository<ShowcaseComment, Long> {

    @EntityGraph(attributePaths = {"author", "parent"})
    List<ShowcaseComment> findAllByShowcaseOrderByIdAsc(PartyShowcase showcase);

    Optional<ShowcaseComment> findByIdAndShowcase(long id, PartyShowcase showcase);
}
