package com.back.domain.notification.notification.controller;

import com.back.domain.notification.notification.dtos.NotificationPageDto;
import com.back.domain.notification.notification.dtos.NotificationReadResponse;
import com.back.domain.notification.notification.service.NotificationService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/notifications")
@Validated
@Tag(name = "ApiV1NotificationController", description = "알림 컨트롤러")
public class ApiV1NotificationController {
    private final NotificationService notificationService;
    private final Rq rq;

    @GetMapping()
    @Operation(
            summary = "내 알림 목록 조회",
            description = """
                    로그인한 회원의 알림을 페이지 단위로 조회한다.
                    isRead를 생략하면 전체를 조회하며, 지정하면 읽음 여부로 필터링한다.
                    알림은 최신순으로 정렬된다.

                    예외
                    - 400-1 : page·size 범위 검증 실패
                    - 401-1 : 미로그인
                    """
    )
    public RsData<NotificationPageDto> get(
            @Parameter(description = "읽음 여부 필터. 생략 시 전체 조회")
            @RequestParam(required = false) Boolean isRead,
            @Parameter(description = "0부터 시작하는 페이지 번호")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기. 1~100, 기본값 20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return new RsData<>(
                "200-1",
                "알림 목록 조회 성공",
                notificationService.getList(rq.getActorFromDb(), isRead, page, size)
                );
    }

    public record NotificationModifyReqBody (
        @NotEmpty List<@NotNull @Positive Long> ids
    ) { }

    @PatchMapping("/read")
    @Operation(
            summary = "알림 읽음 처리",
            description = """
                    로그인한 회원이 소유한 알림들을 읽음 상태로 변경한다.
                    ids에는 중복 없이 하나 이상의 알림 ID를 전달해야 하며, 모든 ID가 본인 알림이어야 한다.

                    예외
                    - 400-1 : ids 누락·빈 목록·중복 ID 또는 형식 검증 실패
                    - 401-1 : 미로그인
                    - 404-1 : 존재하지 않거나 본인 소유가 아닌 알림
                    """
    )
    public RsData<List<NotificationReadResponse>> read(
            @Valid @RequestBody NotificationModifyReqBody request
    ) {

        return new RsData<>(
                "200-1",
                "알림 읽음 처리 성공",
                notificationService.read(rq.getActorFromDb(), request.ids)
        );
    }

    @DeleteMapping
    @Operation(
            summary = "알림 삭제",
            description = """
                    로그인한 회원의 알림을 삭제한다.
                    ids에는 중복 없이 하나 이상의 알림 ID를 전달해야 한다.
                    삭제는 멱등 처리되어, 이미 삭제됐거나 다른 회원의 알림 ID는 무시한다.

                    예외
                    - 400-1 : ids 누락·빈 목록·중복 ID 또는 형식 검증 실패
                    - 401-1 : 미로그인
                    """
    )
    public RsData<Void> delete(
            @Valid @RequestBody NotificationModifyReqBody request
    ) {

        notificationService.delete(rq.getActorFromDb(), request.ids);
        return new RsData<Void>(
                "200-1",
                "알림 삭제 처리 성공",
                null
        );
    }

}
