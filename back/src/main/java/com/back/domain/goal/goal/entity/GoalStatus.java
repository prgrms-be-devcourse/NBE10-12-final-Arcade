package com.back.domain.goal.goal.entity;

public enum GoalStatus {
    WANT,
    IN_PROGRESS,
    HOLD,
    ACHIEVED;

    // 이미 끝난 활동은 WANT를 건너뛰고 바로 ACHIEVED로 등록할 수 있다.
    // ACHIEVED는 종료 상태라 이후 전이를 허용하지 않는다 (되돌리려면 삭제 후 재등록).
    public boolean canTransitionTo(GoalStatus next) {
        return switch (this) {
            case WANT -> next == IN_PROGRESS || next == ACHIEVED;
            case IN_PROGRESS -> next == HOLD || next == ACHIEVED;
            case HOLD -> next == IN_PROGRESS || next == ACHIEVED;
            case ACHIEVED -> false;
        };
    }
}
