package com.back.domain.interaction.like.entity;

public enum TargetType {
    PARTY,
    CONTEST,
    GOAL,
    // 전시 성취의 좋아요, 북마크, 댓글 대상. 모집글(PARTY)과는 별개 지표라
    // 파티 확정 후 별도로 분리했다 - 모집글 좋아요와 전시글 좋아요(완료된 결과물 평가)는 시점도 응답자 의도도 다르다(기획서 3.2)
    PARTY_SHOWCASE
}
