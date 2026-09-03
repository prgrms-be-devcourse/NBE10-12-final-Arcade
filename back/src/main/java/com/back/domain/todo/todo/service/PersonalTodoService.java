package com.back.domain.todo.todo.service;

import com.back.domain.goal.goal.repository.GoalRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.todo.todo.dtos.PersonalTodoDetailDto;
import com.back.domain.todo.todo.dtos.PersonalTodoDto;
import com.back.domain.todo.todo.dtos.PersonalTodoItemDto;
import com.back.domain.todo.todo.dtos.TodoCreateReqBody;
import com.back.domain.todo.todo.dtos.TodoItemReqBody;
import com.back.domain.todo.todo.dtos.TodoProgressDto;
import com.back.domain.todo.todo.dtos.TodoUpdateReqBody;
import com.back.domain.todo.todo.entity.PersonalTodo;
import com.back.domain.todo.todo.entity.PersonalTodoItem;
import com.back.domain.todo.todo.entity.TodoStatus;
import com.back.domain.todo.todo.repository.PersonalTodoItemRepository;
import com.back.domain.todo.todo.repository.PersonalTodoRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 개인 TODO. 성취와 달리 전부 본인 것만 다룬다 - 남에게 보여줄 이유가 없어 조회도 소유자로 제한한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalTodoService {

    /** 상세에 함께 내려주는 항목 첫 페이지 크기 */
    private static final int DETAIL_ITEM_SIZE = 20;

    /** 어떤 id 와도 맞지 않는 값. `in ()` 을 피하려고 쓴다 */
    private static final List<Long> NO_IDS = List.of(-1L);

    private final PersonalTodoRepository personalTodoRepository;
    private final PersonalTodoItemRepository personalTodoItemRepository;
    /**
     * 연결 정보는 성취(PersonalChecklist)가 FK 로 갖는다. TODO 는 자기가 연결됐는지 모르므로 여기서 물어본다.
     * (GoalService 가 PartyRepository 를 직접 주입받는 것과 같은 방식이다.)
     */
    private final GoalRepository goalRepository;

    @Transactional
    public PersonalTodoDto create(Member owner, TodoCreateReqBody request) {
        PersonalTodo todo = personalTodoRepository.save(
                new PersonalTodo(owner, request.title(), request.category(), request.memo())
        );

        // 방금 만든 TODO 라 항목이 없다
        return new PersonalTodoDto(todo, 0, 0);
    }

    /**
     * 목록. 진행률도 연결 여부도 TODO 마다 묻지 않고 한 번에 모아 붙인다.
     *
     * linked 를 false 로 주면 아직 성취에 연결되지 않은 것만 온다 - 성취 등록 화면의 TODO 선택에 쓴다.
     */
    public Page<PersonalTodoDto> getMyTodos(
            Member owner,
            TodoStatus status,
            Boolean linked,
            Pageable pageable
    ) {
        // 연결 여부로 거를 때만 성취 쪽을 읽는다
        List<Long> linkedTodoIds = linked == null
                ? NO_IDS
                : idsForInClause(goalRepository.findLinkedPersonalTodoIds(owner.getId()));

        Page<PersonalTodo> todos = personalTodoRepository.searchMyTodos(
                owner, status, linked, linkedTodoIds, pageable);

        Map<Long, TodoProgressDto> progressByTodoId = loadProgress(todos.getContent());

        return todos.map(todo -> {
            TodoProgressDto progress = progressByTodoId.get(todo.getId());

            return progress == null
                    ? new PersonalTodoDto(todo, 0, 0)
                    : new PersonalTodoDto(todo, progress.totalCount(), progress.doneCount());
        });
    }

    /** `in ()` 은 DB 에 따라 문법 오류라, 비었을 때는 어떤 id 와도 맞지 않는 값을 넣는다 */
    private List<Long> idsForInClause(List<Long> ids) {
        return ids.isEmpty() ? NO_IDS : ids;
    }

    private Map<Long, TodoProgressDto> loadProgress(List<PersonalTodo> todos) {
        if (todos.isEmpty()) return Map.of();

        List<Long> todoIds = todos.stream().map(PersonalTodo::getId).toList();

        return personalTodoItemRepository.findProgressByTodoIds(todoIds).stream()
                .collect(Collectors.toMap(TodoProgressDto::todoId, Function.identity()));
    }

    public PersonalTodoDetailDto getTodo(Member actor, long todoId) {
        PersonalTodo todo = findOwnedTodo(actor, todoId);

        Page<PersonalTodoItem> firstPage = personalTodoItemRepository
                .findAllByTodoOrderBySortOrderAscIdAsc(todo, PageRequest.of(0, DETAIL_ITEM_SIZE));

        return new PersonalTodoDetailDto(
                todo,
                firstPage,
                personalTodoItemRepository.countByTodoAndDoneIsTrue(todo)
        );
    }

    @Transactional
    public PersonalTodoDto update(Member actor, long todoId, TodoUpdateReqBody request) {
        PersonalTodo todo = findOwnedTodo(actor, todoId);

        if (request.status() != null) todo.changeStatus(request.status());
        todo.update(request.title(), request.category(), request.memo());

        // 먼저 내보내야 응답의 modifyDate 가 수정 후 값으로 나간다
        personalTodoRepository.flush();

        return toDto(todo);
    }

    /**
     * 삭제. 연결된 성취가 있으면 FK 를 먼저 끊는다(완료된 성취면 포트가 409-1 로 막는다).
     * cascade 가 없어 항목도 직접 지워야 한다.
     */
    @Transactional
    public void delete(Member actor, long todoId) {
        PersonalTodo todo = findOwnedTodo(actor, todoId);

        // 연결된 성취가 있으면 FK 를 먼저 끊는다. 완료된 성취면 detachTodo() 가 409-1 로 막는다.
        goalRepository.findChecklistByPersonalTodoId(todoId).ifPresent(checklist -> {
            checklist.detachTodo();
            // FK 를 끊는 UPDATE 가 TODO 의 DELETE 보다 먼저 나가야 한다
            goalRepository.flush();
        });
        personalTodoItemRepository.deleteAllByTodo(todo);
        personalTodoRepository.delete(todo);
    }

    public Page<PersonalTodoItemDto> getItems(Member actor, long todoId, Pageable pageable) {
        PersonalTodo todo = findOwnedTodo(actor, todoId);

        return personalTodoItemRepository
                .findAllByTodoOrderBySortOrderAscIdAsc(todo, pageable)
                .map(PersonalTodoItemDto::new);
    }

    @Transactional
    public PersonalTodoItemDto addItem(Member actor, long todoId, TodoItemReqBody request) {
        PersonalTodo todo = findOwnedTodo(actor, todoId);

        PersonalTodoItem item = personalTodoItemRepository.save(new PersonalTodoItem(
                todo, request.content(), personalTodoItemRepository.nextSortOrder(todo)
        ));

        return new PersonalTodoItemDto(item);
    }

    @Transactional
    public PersonalTodoItemDto updateItem(Member actor, long todoId, long itemId, TodoItemReqBody request) {
        PersonalTodoItem item = findOwnedItem(actor, todoId, itemId);
        item.updateContent(request.content());
        personalTodoItemRepository.flush();

        return new PersonalTodoItemDto(item);
    }

    /** 완료. 멱등이라 두 번 눌러도 완료 시각이 밀리지 않는다. */
    @Transactional
    public PersonalTodoItemDto completeItem(Member actor, long todoId, long itemId) {
        PersonalTodoItem item = findOwnedItem(actor, todoId, itemId);
        item.complete();
        personalTodoItemRepository.flush();

        return new PersonalTodoItemDto(item);
    }

    @Transactional
    public void deleteItem(Member actor, long todoId, long itemId) {
        personalTodoItemRepository.delete(findOwnedItem(actor, todoId, itemId));
    }

    private PersonalTodo findOwnedTodo(Member actor, long todoId) {
        PersonalTodo todo = personalTodoRepository.findById(todoId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 개인 TODO입니다."));
        todo.checkOwnedBy(actor);

        return todo;
    }

    /** 항목은 소유자 판정을 부모에서 받는다. 경로의 todoId 와 다른 TODO 의 항목이면 404. */
    private PersonalTodoItem findOwnedItem(Member actor, long todoId, long itemId) {
        PersonalTodo todo = findOwnedTodo(actor, todoId);

        PersonalTodoItem item = personalTodoItemRepository.findById(itemId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 할 일입니다."));

        if (!item.getTodo().getId().equals(todo.getId())) {
            throw new ServiceException("404-1", "존재하지 않는 할 일입니다.");
        }

        return item;
    }

    private PersonalTodoDto toDto(PersonalTodo todo) {
        return new PersonalTodoDto(
                todo,
                personalTodoItemRepository.countByTodo(todo),
                personalTodoItemRepository.countByTodoAndDoneIsTrue(todo)
        );
    }
}
