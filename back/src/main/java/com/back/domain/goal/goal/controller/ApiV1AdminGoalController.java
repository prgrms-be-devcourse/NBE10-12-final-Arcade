package com.back.domain.goal.goal.controller;

import com.back.domain.goal.goal.dtos.AdminEvidenceDto;
import com.back.domain.goal.goal.dtos.EvidenceFileDto;
import com.back.domain.goal.goal.entity.EvidenceStatus;
import com.back.domain.goal.goal.service.GoalService;
import com.back.global.dto.PageDto;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 성취 증빙 검수. 관리자 전용이다.
 *
 * 권한은 SecurityConfig 의 `/api/*&#47;adm/**` -> hasRole("ADMIN") 규칙이 건다.
 */
@RestController
@RequestMapping("/api/v1/adm/goals")
@RequiredArgsConstructor
@Tag(name = "ApiV1AdminGoalController", description = "관리자 성취 증빙 검수 컨트롤러")
public class ApiV1AdminGoalController {

    private final GoalService goalService;

    @GetMapping("/evidences")
    @Operation(
            summary = "증빙 검수 목록",
            description = """
                    증빙 파일이 올라온 성취를 검수 상태로 걸러 본다. 기본은 검수 대기(PENDING)다.

                    파일 자체는 목록에 없다 - 내용은 아래 다운로드로 확인한다.
                    오래 기다린 건부터 보도록 마지막 변경 시각 오름차순으로 정렬한다.

                    예외
                    - 401-1 : 미로그인
                    - 403-1 : 관리자가 아님
                    """
    )
    public RsData<PageDto<AdminEvidenceDto>> listEvidences(
            @RequestParam(defaultValue = "PENDING") EvidenceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return new RsData<>(
                "200-1",
                "증빙 검수 목록 조회 성공",
                goalService.getEvidencesForAdmin(
                        status,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "modifyDate"))
                )
        );
    }

    @GetMapping("/{goalId}/evidence")
    @Operation(
            summary = "증빙 파일 다운로드",
            description = """
                    증빙 파일을 내려받는다. **관리자만 받을 수 있다** -
                    실명·소속이 찍힌 문서라 사용자 API 에는 다운로드가 없다.

                    저장된 형식과 무관하게 항상 첨부파일(application/octet-stream)로 내려준다.
                    업로더가 보낸 Content-Type 을 그대로 실으면 우리 도메인에서 실행될 수 있다.

                    예외
                    - 401-1 : 미로그인
                    - 403-1 : 관리자가 아님
                    - 404-1 : 존재하지 않는 성취이거나 증빙 파일이 없는 성취
                    """
    )
    public ResponseEntity<Resource> downloadEvidence(@PathVariable long goalId) {
        EvidenceFileDto file = goalService.readEvidenceForAdmin(goalId);

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                // 파일명은 업로더가 정한 값이다. filename(name, UTF_8) 이 인코딩·escape 를 처리한다.
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName() == null ? "evidence" : file.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString());

        if (file.size() != null) response.contentLength(file.size());

        return response.body(new InputStreamResource(file.content()));
    }

    public record ReviewReqBody(
            @NotNull EvidenceStatus status,
            /** 반려 사유. 반려일 때는 필수다. 승인이면 무시하고 이전 사유를 지운다 */
            String note
    ) { }

    @PatchMapping("/{goalId}/evidence")
    @Operation(
            summary = "증빙 승인 · 반려",
            description = """
                    검수 결과를 남긴다. status 는 APPROVED 또는 REJECTED 다.

                    반려하려면 note(사유)가 필요하다 - 올린 사람이 무엇을 고쳐 다시 올려야 하는지
                    알아야 하기 때문이다. 사유는 그 사람의 성취 상세에만 내려간다.

                    승인한 뒤라도 **파일을 다시 올리면 PENDING 으로 되돌아간다** -
                    승인 뒤 파일만 바꿔치기하는 걸 막는다.

                    예외
                    - 400-1 : status 가 APPROVED/REJECTED 가 아니거나, 반려인데 사유가 없음
                    - 401-1 : 미로그인
                    - 403-1 : 관리자가 아님
                    - 404-1 : 존재하지 않는 성취이거나 증빙 파일이 없는 성취
                    """
    )
    public RsData<Void> reviewEvidence(
            @PathVariable long goalId,
            @Valid @RequestBody ReviewReqBody request
    ) {
        goalService.reviewEvidence(goalId, request.status(), request.note());

        return new RsData<>(
                "200-1",
                request.status() == EvidenceStatus.APPROVED ? "증빙 승인 성공" : "증빙 반려 성공"
        );
    }
}
