package com.back.domain.search.search.entity.party;

import com.back.domain.party.party.entity.Party;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class PartySearchKeyword extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false, unique = true)
    private Party party;

    private String keywords;

    public PartySearchKeyword(Party party, String keywords) {
        this.party = party;
        this.keywords = keywords;
    }

    public void updateKeywords(String keywords) {
        this.keywords = keywords;
    }
}
