package com.back.domain.party.party.dtos;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.position.entity.PartyStatus;

import java.time.LocalDateTime;

/**
 * 마이페이지 '참여 파티 히스토리' 한 줄 (기획서 2.11).
 *
 * 이전에는 이 화면을 성취(GET /goals/me)의 PROJECT 로 그렸는데, 그건 성취 리스트용 응답이라
 * 기획서가 요구한 **주제 유형**과 **역할(파티장/파티원)** 이 없었다. 그래서 파티를 직접 조회한다.
 *
 * 확정 명단 기준이라 모집 중인 파티는 담기지 않는다 - status 는 IN_PROGRESS 아니면 COMPLETED 다.
 */
public record MyPartyDto(
        long partyId,
        String partyName,
        /** 주제 유형. 화면이 아이콘으로 그린다 - CONTEST(대회) / PROJECT / STUDY / ETC */
        TopicType topicType,
        /** 파티 진행 상태. 확정된 파티만 담기므로 IN_PROGRESS(진행중) 아니면 COMPLETED(완료) 다 */
        PartyStatus status,
        /** 이 파티에서 내 역할 */
        MyPartyRole role,
        /**
         * 이 파티에서 내가 맡은 자리. 파티장은 지원 절차가 없어 null 이다.
         * (파티장의 대표 포지션은 프로필에 있고, 파티마다 다르지 않다)
         */
        PositionType positionType,
        /** 파티 개설 시각. 기간 문구의 시작점이다 */
        LocalDateTime createdAt,
        /** 완료 시각. 모집 중이거나 진행 중이면 null 이다 */
        LocalDateTime completedAt,
        /** 파티장이 전시글을 게시했는지. 게시된 것만 전시 페이지로 연결한다(기획서 2.11) */
        boolean exhibited
) {
    public enum MyPartyRole {
        OWNER, MEMBER
    }

    public MyPartyDto(Party party, MyPartyRole role, PositionType positionType, boolean exhibited) {
        this(
                party.getId(),
                party.getPartyName(),
                party.getTopicType(),
                party.getStatus(),
                role,
                positionType,
                party.getCreateDate(),
                party.getCompletedAt(),
                exhibited
        );
    }
}
