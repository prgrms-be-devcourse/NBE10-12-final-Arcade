package com.back.domain.todo.todo.controller;

import com.back.domain.todo.todo.dtos.PersonalTodoDetailDto;
import com.back.domain.todo.todo.dtos.PersonalTodoDto;
import com.back.domain.todo.todo.dtos.PersonalTodoItemDto;
import com.back.domain.todo.todo.dtos.TodoCreateReqBody;
import com.back.domain.todo.todo.dtos.TodoItemReqBody;
import com.back.domain.todo.todo.dtos.TodoUpdateReqBody;
import com.back.domain.todo.todo.entity.TodoStatus;
import com.back.domain.todo.todo.service.PersonalTodoService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
@Validated
@Tag(name = "ApiV1PersonalTodoController", description = "개인 TODO 컨트롤러")
public class ApiV1PersonalTodoController {

    private final PersonalTodoService personalTodoService;
    private final Rq rq;

    @PostMapping
    @Operation(
            summary = "개인 TODO 등록",
            description = """
                    큰 주제 하나를 만든다. 할 일 항목은 여기서 받지 않고, 상세 화면에서 한 건씩 추가한다.
                    상태는 받지 않는다 - 새 TODO는 항상 WANT로 시작한다.

                    예외
                    - 400-1 : title 누락
                    - 400-2 : category 가 정의된 값이 아님
                    - 401-1 : 미로그인
                    """
    )
    public RsData<PersonalTodoDto> create(
            @Valid @RequestBody TodoCreateReqBody request
    ) {
        return new RsData<>(
                "201-1",
                "개인 TODO 등록 성공",
                personalTodoService.create(rq.getActorFromDb(), request)
        );
    }

    @GetMapping("/me")
    @Operation(
            summary = "내 개인 TODO 목록",
            description = """
                    본인 것만 돌려준다. 최신순이며 status·linked 는 모두 선택이다.
                    각 항목에 진행률(totalCount·doneCount)과 성취 연결 여부(linked)가 함께 온다.

                    linked 는 저장된 값이 아니라 조회 때 계산한다 - 연결 정보는 성취(PersonalChecklist)가 FK 로 갖는다.
                    linked=false 를 주면 아직 성취에 연결되지 않은 것만 온다. 성취 등록 화면의 TODO 선택에 쓴다.

                    예외
                    - 400-1 : status 가 정의된 값이 아니거나 page·size 범위 밖
                    - 401-1 : 미로그인
                    """
    )
    public RsData<Page<PersonalTodoDto>> getMyTodos(
            @Parameter(description = "진행 상태 필터")
            @RequestParam(required = false) TodoStatus status,

            @Parameter(description = "성취 연결 여부 필터. false 면 아직 연결되지 않은 것만")
            @RequestParam(required = false) Boolean linked,

            @Parameter(description = "0부터 시작하는 페이지 번호")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "페이지 크기. 1~100, 기본값 20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return new RsData<>(
                "200-1",
                "개인 TODO 목록 조회 성공",
                personalTodoService.getMyTodos(rq.getActorFromDb(), status, linked, PageRequest.of(page, size))
        );
    }

    @GetMapping("/{todoId}")
    @Operation(
            summary = "개인 TODO 상세",
            description = """
                    본인 것만 볼 수 있다. 성취와 달리 공개 대상이 아니다.
                    items 는 첫 페이지(20건)만 담기고, hasMoreItems 가 true 면 항목 목록 API로 이어 받는다.

                    예외
                    - 401-1 : 미로그인
                    - 403-1 : 남의 TODO
                    - 404-1 : 존재하지 않는 TODO
                    """
    )
    public RsData<PersonalTodoDetailDto> getTodo(
            @PathVariable long todoId
    ) {
        return new RsData<>(
                "200-1",
                "개인 TODO 상세 조회 성공",
                personalTodoService.getTodo(rq.getActorFromDb(), todoId)
        );
    }

    @PatchMapping("/{todoId}")
    @Operation(
            summary = "개인 TODO 수정",
            description = """
                    부분 수정이다. 넘기지 않은(null) 필드는 그대로 두고, 빈 문자열을 보내면 비운다.
                    성취 수정이 detail 을 통째로 교체하는 것과 다른데, 화면이 메모·상태를 따로 저장하기 때문이다.

                    상태 전이에 제약이 없다 - 완료한 TODO를 다시 열 수 있다.

                    예외
                    - 400-2 : category·status 가 정의된 값이 아님
                    - 400-4 : title 을 빈 값으로 보냄
                    - 401-1 : 미로그인
                    - 403-1 : 남의 TODO
                    - 404-1 : 존재하지 않는 TODO
                    """
    )
    public RsData<PersonalTodoDto> update(
            @PathVariable long todoId,
            @Valid @RequestBody TodoUpdateReqBody request
    ) {
        return new RsData<>(
                "200-1",
                "개인 TODO 수정 성공",
                personalTodoService.update(rq.getActorFromDb(), todoId, request)
        );
    }

    @DeleteMapping("/{todoId}")
    @Operation(
            summary = "개인 TODO 삭제",
            description = """
                    할 일 항목도 함께 지워진다.

                    성취에 연결돼 있으면 성취의 연결만 끊고 TODO를 지운다.
                    그 성취는 진행 과정을 잃으므로 화면이 미리 알려야 한다.
                    다만 연결된 성취가 이미 완료(ACHIEVED)됐다면 409-1로 막는다 - 완료의 근거가 사라지기 때문이다.

                    예외
                    - 401-1 : 미로그인
                    - 403-1 : 남의 TODO
                    - 404-1 : 존재하지 않는 TODO
                    - 409-1 : 완료된 성취에 연결돼 있음
                    """
    )
    public RsData<Void> delete(
            @PathVariable long todoId
    ) {
        personalTodoService.delete(rq.getActorFromDb(), todoId);

        return new RsData<>("204-1", "개인 TODO 삭제 성공", null);
    }

    @GetMapping("/{todoId}/items")
    @Operation(
            summary = "할 일 목록",
            description = """
                    저장된 표시 순서대로 온다. 진행률은 상세 응답의 totalCount·doneCount 를 쓴다 -
                    이 응답은 현재 페이지만 담아서 여기서는 셀 수 없다.

                    예외
                    - 400-1 : page·size 범위 밖
                    - 401-1 : 미로그인
                    - 403-1 : 남의 TODO
                    - 404-1 : 존재하지 않는 TODO
                    """
    )
    public RsData<Page<PersonalTodoItemDto>> getItems(
            @PathVariable long todoId,

            @Parameter(description = "0부터 시작하는 페이지 번호")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "페이지 크기. 1~100, 기본값 20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return new RsData<>(
                "200-1",
                "할 일 목록 조회 성공",
                personalTodoService.getItems(rq.getActorFromDb(), todoId, PageRequest.of(page, size))
        );
    }

    @PostMapping("/{todoId}/items")
    @Operation(
            summary = "할 일 추가",
            description = """
                    맨 뒤에 붙는다. 순서를 바꾸는 API는 없다 - 화면에도 그 UI가 없다.

                    예외
                    - 400-1 : content 누락
                    - 401-1 : 미로그인
                    - 403-1 : 남의 TODO
                    - 404-1 : 존재하지 않는 TODO
                    """
    )
    public RsData<PersonalTodoItemDto> addItem(
            @PathVariable long todoId,
            @Valid @RequestBody TodoItemReqBody request
    ) {
        return new RsData<>(
                "201-1",
                "할 일 추가 성공",
                personalTodoService.addItem(rq.getActorFromDb(), todoId, request)
        );
    }

    @PatchMapping("/{todoId}/items/{itemId}")
    @Operation(
            summary = "할 일 내용 수정",
            description = """
                    내용만 바꾼다. 완료 여부는 완료 API로 처리한다.

                    예외
                    - 400-1 : content 누락
                    - 401-1 : 미로그인
                    - 403-1 : 남의 TODO
                    - 404-1 : 존재하지 않는 TODO 또는 할 일
                    """
    )
    public RsData<PersonalTodoItemDto> updateItem(
            @PathVariable long todoId,
            @PathVariable long itemId,
            @Valid @RequestBody TodoItemReqBody request
    ) {
        return new RsData<>(
                "200-1",
                "할 일 수정 성공",
                personalTodoService.updateItem(rq.getActorFromDb(), todoId, itemId, request)
        );
    }

    @PostMapping("/{todoId}/items/{itemId}/complete")
    @Operation(
            summary = "할 일 완료",
            description = """
                    완료 시각(doneAt)이 찍힌다. 멱등이라 두 번 불러도 처음 시각이 유지된다.

                    되돌리는 API는 없다 - 화면에도 되돌리기 UI가 없다.

                    예외
                    - 401-1 : 미로그인
                    - 403-1 : 남의 TODO
                    - 404-1 : 존재하지 않는 TODO 또는 할 일
                    """
    )
    public RsData<PersonalTodoItemDto> completeItem(
            @PathVariable long todoId,
            @PathVariable long itemId
    ) {
        return new RsData<>(
                "200-1",
                "할 일 완료 처리 성공",
                personalTodoService.completeItem(rq.getActorFromDb(), todoId, itemId)
        );
    }

    @DeleteMapping("/{todoId}/items/{itemId}")
    @Operation(
            summary = "할 일 삭제",
            description = """
                    예외
                    - 401-1 : 미로그인
                    - 403-1 : 남의 TODO
                    - 404-1 : 존재하지 않는 TODO 또는 할 일
                    """
    )
    public RsData<Void> deleteItem(
            @PathVariable long todoId,
            @PathVariable long itemId
    ) {
        personalTodoService.deleteItem(rq.getActorFromDb(), todoId, itemId);

        return new RsData<>("204-1", "할 일 삭제 성공", null);
    }
}
