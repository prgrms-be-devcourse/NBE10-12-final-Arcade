package com.back.domain.message.message.controller;

import com.back.domain.message.message.dtos.MessageDetailDto;
import com.back.domain.message.message.dtos.MessageDto;
import com.back.domain.message.message.dtos.MessageListDto;
import com.back.domain.message.message.dtos.MessagePageDto;
import com.back.domain.message.message.service.MessageService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "ApiV1MessageController", description = "쪽지 컨트롤러")
public class ApiV1MessageController {
    private final MessageService messageService;
    private final Rq rq;

    public record MessageReqBody(@NotBlank @Size(max = 1000) String content) { }

    public record MessageReadReqBody(@NotEmpty List<@NotNull @Positive Long> ids) { }

    @PostMapping("/{memberId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "쪽지 발송",
            description = """
                    지정한 회원에게 쪽지를 발송한다. content는 공백이 아니며 최대 1,000자까지 입력할 수 있다.

                    예외
                    - 400-1 : content 누락·공백 또는 1,000자 초과
                    - 401-1 : 미로그인
                    - 404-1 : 존재하지 않는 수신 회원
                    - 409-1 : 본인에게 쪽지 발송 시도
                    """
    )
    public RsData<MessageDto> send(
            @Parameter(description = "수신 회원 ID") @PathVariable long memberId,
            @Valid @RequestBody MessageReqBody request
    ) {
        return new RsData<>("201-1", "쪽지 발송 성공",
                messageService.send(rq.getActorFromDb(), memberId, request.content));
    }

    @GetMapping("/me/messages")
    @Operation(
            summary = "내 쪽지함 조회",
            description = """
                    로그인한 회원의 받은 쪽지 또는 보낸 쪽지를 페이지 단위로 조회한다.
                    box를 생략하면 RECEIVED(받은 쪽지)를 조회하며, 최신순으로 정렬된다.

                    예외
                    - 400-1 : box 값이 RECEIVED 또는 SENT가 아님, page·size 범위 검증 실패
                    - 401-1 : 미로그인
                    """
    )
    public RsData<MessagePageDto> getList(
            @Parameter(description = "쪽지함 종류: RECEIVED(받은 쪽지), SENT(보낸 쪽지)")
            @Valid @RequestParam(defaultValue = "RECEIVED") MessageService.MessageFilterOption box,
            @Parameter(description = "0부터 시작하는 페이지 번호")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기. 1~100, 기본값 20")
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
    @Operation(
            summary = "쪽지 읽음 처리",
            description = """
                    로그인한 회원이 수신한 쪽지들을 읽음 상태로 변경한다.
                    ids에는 중복 없이 하나 이상의 쪽지 ID를 전달해야 하며, 보낸 쪽지는 읽음 처리할 수 없다.

                    예외
                    - 400-1 : ids 누락·빈 목록·중복 ID 또는 형식 검증 실패
                    - 401-1 : 미로그인
                    - 403-1 : 수신자가 아닌 쪽지의 읽음 처리 시도
                    - 404-1 : 존재하지 않는 쪽지
                    """
    )
    public RsData<List<MessageDto>> read(@Valid @RequestBody MessageReadReqBody request) {
        return new RsData<>("200-1", "편지 읽음 처리 성공",
                messageService.read(rq.getActorFromDb(), request.ids));
    }

    @GetMapping("/me/messages/{messageId}")
    @Operation(
            summary = "쪽지 상세 조회",
            description = """
                    로그인한 회원이 보냈거나 받은 쪽지의 상세 내용을 조회한다.
                    수신자가 조회하면 해당 쪽지는 자동으로 읽음 처리된다.

                    예외
                    - 401-1 : 미로그인
                    - 403-1 : 본인과 관련 없는 쪽지 조회 시도
                    - 404-1 : 존재하지 않는 쪽지
                    """
    )
    public RsData<MessageDetailDto> getMessage(
            @Parameter(description = "조회할 쪽지 ID") @PathVariable long messageId
    ) {
        return new RsData<>("200-1", "쪽지 상세 조회 성공",
                messageService.getMessage(rq.getActorFromDb(), messageId));
    }
}
