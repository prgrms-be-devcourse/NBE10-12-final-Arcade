package com.back.domain.contest.contest.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Contest extends BaseEntity {
    @Column(name = "host_id")
    private Long hostId;

    @Column(name = "creator_member_id")
    private Long creatorMemberId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContestTag contestTag;

    @Column(nullable = false)
    private LocalDate applicationPeriodStart;

    @Column(nullable = false)
    private LocalDate applicationPeriodEnd;

    public Contest(
        Long creatorMemberId,
        String title,
        ContestFormat format,
        ContestTag contestTag,
        LocalDate applicationPeriodStart,
        LocalDate applicationPeriodEnd
    ) {
        this.creatorMemberId = creatorMemberId;
        this.title = title;
        this.format = format;
        this.contestTag = contestTag;
        this.applicationPeriodStart = applicationPeriodStart;
        this.applicationPeriodEnd = applicationPeriodEnd;

    }
    public void modifyPeriod(LocalDate start, LocalDate end) {
        this.applicationPeriodStart = start;
        this.applicationPeriodEnd = end;
    }


}
