package com.back.domain.message.message.controller;

import com.back.domain.message.message.dtos.MessageDetailDto;
import com.back.domain.message.message.dtos.MessageDto;
import com.back.domain.message.message.dtos.MessageListDto;
import com.back.domain.message.message.dtos.MessagePageDto;
import com.back.domain.message.message.service.MessageService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Validated
public class ApiV1MessageController {
    private final MessageService messageService;
    private final Rq rq;

    public record MessageReqBody(@NotBlank @Size(max = 1000) String content) { }

    public record MessageReadReqBody(@NotEmpty List<@NotNull @Positive Long> ids) { }

    @PostMapping("/{memberId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public RsData<MessageDto> send(@PathVariable long memberId, @Valid @RequestBody MessageReqBody request) {
        return new RsData<>("201-1", "쪽지 발송 성공",
                messageService.send(rq.getActorFromDb(), memberId, request.content));
    }

    @GetMapping("/me/messages")
    public RsData<MessagePageDto> getList(
            @Valid @RequestParam(defaultValue = "RECEIVED") MessageService.MessageFilterOption box,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {

        return new RsData<>(
                "200-1",
                "쪽지함 조회 성공",
                messageService.getList(
                        rq.getActorFromDb(), box, page, size)
        );
    }

    @PatchMapping("/me/messages")
    public RsData<List<MessageDto>> read(@Valid @RequestBody MessageReadReqBody request) {
        return new RsData<>("200-1", "편지 읽음 처리 성공",
                messageService.read(rq.getActorFromDb(), request.ids));
    }

    @GetMapping("/me/messages/{messageId}")
    public RsData<MessageDetailDto> getMessage(@PathVariable long messageId) {
        return new RsData<>("200-1", "쪽지 상세 조회 성공",
                messageService.getMessage(rq.getActorFromDb(), messageId));
    }
}
