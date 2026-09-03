package com.back.domain.todo.todo.repository;

/**
 * TODO 삭제 시 연결된 성취를 끊는 통로. 구현은 성취 도메인에 있다.
 * 성취가 TODO를 FK로 가리키므로, 반대 방향을 직접 참조하면 순환이 된다.
 */
public interface TodoLinkedGoalPort {

    /** 연결된 성취가 있으면 FK를 끊는다. 완료된 성취면 409-1. */
    void detachFromTodo(long todoId);
}
