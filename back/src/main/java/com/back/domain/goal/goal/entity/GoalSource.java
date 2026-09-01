package com.back.domain.goal.goal.entity;

// 사용자가 직접 등록했는지(SELF_REPORTED), 크루온 활동으로 자동 기록됐는지(PLATFORM_VERIFIED) 구분
public enum GoalSource {
    SELF_REPORTED,
    PLATFORM_VERIFIED
}
