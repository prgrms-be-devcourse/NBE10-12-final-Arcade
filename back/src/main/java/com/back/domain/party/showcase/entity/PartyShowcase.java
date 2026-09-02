package com.back.domain.party.showcase.entity;

import com.back.domain.party.party.entity.Party;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class PartyShowcase extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false, unique = true)
    private Party party;

    private String title;

    @Lob
    private String description;

    @Column(nullable = false)
    private boolean published;

    private LocalDateTime publishedAt;

    public PartyShowcase(Party party) {
        this.party = party;
        this.published = false;
    }

    public void publish(String title, String description) {
        this.title = title;
        this.description = description;
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }
}
