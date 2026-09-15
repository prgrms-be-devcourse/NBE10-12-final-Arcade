package com.back.domain.todo.todo.entity;

/**
 * 개인 TODO의 분류.
 *
 * 값의 기준은 서버다. 프론트가 등록 모달과 목록 목 데이터에서 서로 다른 목록을 쓰고 있었는데,
 * 실제 데이터에 쓰이던 목록 쪽으로 통일했다.
 */
public enum TodoCategory {
    STUDY,        // 학습
    SIDE,         // 사이드
    CAREER,       // 커리어
    CERTIFICATE,  // 자격증
    ETC           // 기타
}
