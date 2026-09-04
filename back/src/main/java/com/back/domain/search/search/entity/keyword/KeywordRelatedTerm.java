package com.back.domain.search.search.entity.keyword;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        indexes = @Index(name = "idx_keyword_related_term_term", columnList = "term"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"term", "related_term"})
)
public class KeywordRelatedTerm extends BaseEntity {

    private String term;

    private String relatedTerm;

    public KeywordRelatedTerm(String term, String relatedTerm) {
        this.term = term;
        this.relatedTerm = relatedTerm;
    }
}
