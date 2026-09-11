package com.back.domain.goal.goal.entity;

/** 증빙 파일의 검수 상태. 파일이 없으면 null 이다. */
public enum EvidenceStatus {
    /** 올라왔고 아직 관리자가 보지 않았다 */
    PENDING,
    APPROVED,
    REJECTED
}
