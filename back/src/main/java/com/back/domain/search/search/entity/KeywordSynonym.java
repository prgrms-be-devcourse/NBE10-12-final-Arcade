package com.back.domain.search.search.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "term"))
public class KeywordSynonym extends BaseEntity {

    @Column(nullable = false)
    private String term;

    @Column(nullable = false)
    private String canonicalTerm;

    public KeywordSynonym(String term, String canonicalTerm) {
        this.term = term;
        this.canonicalTerm = canonicalTerm;
    }
}
