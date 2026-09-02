package com.back.domain.notification.notification.controller;

import com.back.domain.notification.notification.dtos.NotificationPageDto;
import com.back.domain.notification.notification.dtos.NotificationReadResponse;
import com.back.domain.notification.notification.service.NotificationService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
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
public class ApiV1NotificationController {
    private final NotificationService notificationService;
    private final Rq rq;

    @GetMapping()
    public RsData<NotificationPageDto> get(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") @Min(0) int page,
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
