package com.back.domain.notification.notification.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.notification.notification.entity.Notification;
import com.back.domain.notification.notification.entity.NotificationType;
import com.back.domain.notification.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1NotificationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member(String email) {
        return memberRepository.findByEmail(email).orElseThrow();
    }

    private Notification saveNotification(String recipientEmail, String content) {
        return notificationRepository.save(new Notification(
                member(recipientEmail),
                NotificationType.PARTY_APPLICATION_APPROVED,
                content
        ));
    }

    @Test
    @DisplayName("알림 목록 조회: 로그인 회원의 알림만 페이지 정보와 함께 반환한다")
    @WithUserDetails("user1@test.com")
    void getList() throws Exception {
        Notification notification = saveNotification("user1@test.com", "지원한 파티에 승인되었습니다.");
        saveNotification("user2@test.com", "다른 회원의 알림입니다.");

        ResultActions resultActions = mvc.perform(get("/api/v1/notifications")
                .param("page", "0")
                .param("size", "20"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("알림 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(notification.getId()))
                .andExpect(jsonPath("$.data.content[0].type").value("PARTY_APPLICATION_APPROVED"))
                .andExpect(jsonPath("$.data.content[0].content").value("지원한 파티에 승인되었습니다."))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false))
                .andExpect(jsonPath("$.data.content[0].createAt").isNotEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("알림 목록 조회: isRead 필터를 적용한다")
    @WithUserDetails("user1@test.com")
    void getListWithIsReadFilter() throws Exception {
        Notification unread = saveNotification("user1@test.com", "읽지 않은 알림");
        Notification read = saveNotification("user1@test.com", "읽은 알림");
        read.read();
        entityManager.flush();

        mvc.perform(get("/api/v1/notifications").param("isRead", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(unread.getId()));

        mvc.perform(get("/api/v1/notifications").param("isRead", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(read.getId()));
    }

    @Test
    @DisplayName("알림 목록 조회: page 또는 size가 범위를 벗어나면 400-1이다")
    @WithUserDetails("user1@test.com")
    void getListWithInvalidPageOrSize() throws Exception {
        mvc.perform(get("/api/v1/notifications").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        mvc.perform(get("/api/v1/notifications").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("알림 목록 조회: 로그인하지 않으면 401-1이다")
    void getListWithoutLogin() throws Exception {
        mvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("알림 읽음 처리: 요청한 본인 알림을 읽음으로 처리하고 결과를 반환한다")
    @WithUserDetails("user1@test.com")
    void read() throws Exception {
        Notification first = saveNotification("user1@test.com", "첫 번째 알림");
        Notification second = saveNotification("user1@test.com", "두 번째 알림");

        mvc.perform(patch("/api/v1/notifications/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ids\": [%d, %d] }".formatted(first.getId(), second.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("알림 읽음 처리 성공"))
                .andExpect(jsonPath("$.data[0].id").value(first.getId()))
                .andExpect(jsonPath("$.data[0].isRead").value(true))
                .andExpect(jsonPath("$.data[1].id").value(second.getId()))
                .andExpect(jsonPath("$.data[1].isRead").value(true));

        entityManager.flush();
        entityManager.clear();
        assertThat(notificationRepository.findById(first.getId()).orElseThrow().isRead()).isTrue();
        assertThat(notificationRepository.findById(second.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test
    @DisplayName("알림 읽음 처리: ids가 비어 있거나 유효하지 않으면 400-1이다")
    @WithUserDetails("user1@test.com")
    void readWithInvalidIds() throws Exception {
        mvc.perform(patch("/api/v1/notifications/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ids\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        mvc.perform(patch("/api/v1/notifications/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ids\": [0] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("알림 읽음 처리: 존재하지 않거나 본인 소유가 아닌 알림이 포함되면 404-1이다")
    @WithUserDetails("user1@test.com")
    void readWithUnavailableNotification() throws Exception {
        Notification otherMembersNotification = saveNotification("user2@test.com", "다른 회원의 알림");

        mvc.perform(patch("/api/v1/notifications/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ids\": [999999] }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));

        mvc.perform(patch("/api/v1/notifications/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ids\": [%d] }".formatted(otherMembersNotification.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("알림 읽음 처리: 로그인하지 않으면 401-1이다")
    void readWithoutLogin() throws Exception {
        mvc.perform(patch("/api/v1/notifications/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ids\": [1] }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("알림 삭제: 요청한 본인 알림을 삭제하고 data는 null이다")
    @WithUserDetails("user1@test.com")
    void deleteNotifications() throws Exception {
        Notification notification = saveNotification("user1@test.com", "삭제할 알림");

        mvc.perform(delete("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ids\": [%d] }".formatted(notification.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("알림 삭제 처리 성공"))
                .andExpect(jsonPath("$.data").doesNotExist());

        entityManager.flush();
        entityManager.clear();
        assertThat(notificationRepository.findById(notification.getId())).isEmpty();
    }

    @Test
    @DisplayName("알림 삭제: ids가 비어 있으면 400-1이다")
    @WithUserDetails("user1@test.com")
    void deleteWithEmptyIds() throws Exception {
        mvc.perform(delete("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ids\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("알림 삭제: 로그인하지 않으면 401-1이다")
    void deleteWithoutLogin() throws Exception {
        mvc.perform(delete("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ids\": [1] }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }
}
