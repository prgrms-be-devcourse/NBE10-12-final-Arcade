package com.back.domain.message.message.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.domain.message.message.entity.Message;
import com.back.domain.message.message.repository.MessageRepository;
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
import jakarta.persistence.EntityManager;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1MessageControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member(String email) {
        return memberRepository.findByEmail(email).orElseThrow();
    }

    private Message saveMessage(String senderEmail, String recipientEmail, String content) {
        return messageRepository.save(new Message(member(senderEmail), member(recipientEmail), content));
    }

    private void saveProfile(String email, String nickname) {
        Member member = member(email);
        if (memberProfileRepository.findByMember(member).isEmpty()) {
            memberProfileRepository.save(new MemberProfile(member, nickname, null, List.of(), List.of()));
        }
    }

    @Test
    @DisplayName("쪽지 발송: 로그인한 회원이 다른 회원에게 발송하면 201-1과 쪽지를 반환한다")
    @WithUserDetails("user1@test.com")
    void send() throws Exception {
        Member sender = member("user1@test.com");
        Member recipient = member("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/members/{memberId}/messages", recipient.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "content": "프로젝트 관련해서 질문드리고 싶습니다." }
                        """));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("쪽지 발송 성공"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.senderId").value(sender.getId()))
                .andExpect(jsonPath("$.data.recipientId").value(recipient.getId()))
                .andExpect(jsonPath("$.data.content").value("프로젝트 관련해서 질문드리고 싶습니다."))
                .andExpect(jsonPath("$.data.isRead").value(false))
                .andExpect(jsonPath("$.data.createAt").isNotEmpty());
    }

    @Test
    @DisplayName("쪽지 발송: 로그인하지 않으면 401-1이다")
    void sendWithoutLogin() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"content\": \"안녕하세요\" }"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("쪽지 발송: 존재하지 않는 수신자면 404-1이다")
    @WithUserDetails("user1@test.com")
    void sendToNonexistentRecipient() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/members/999999/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"content\": \"안녕하세요\" }"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 회원입니다."));
    }

    @Test
    @DisplayName("쪽지 발송: 본인에게 발송하면 409-1이다")
    @WithUserDetails("user1@test.com")
    void sendToSelf() throws Exception {
        long memberId = member("user1@test.com").getId();

        ResultActions resultActions = mvc.perform(post("/api/v1/members/{memberId}/messages", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"content\": \"안녕하세요\" }"));

        resultActions.andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"))
                .andExpect(jsonPath("$.msg").value("본인에게는 쪽지를 보낼 수 없습니다."));
    }

    @Test
    @DisplayName("쪽지 발송: 내용이 없으면 400-1이다")
    @WithUserDetails("user1@test.com")
    void sendWithBlankContent() throws Exception {
        long recipientId = member("user2@test.com").getId();

        ResultActions resultActions = mvc.perform(post("/api/v1/members/{memberId}/messages", recipientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"content\": \"\" }"));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("쪽지 발송: 잘못된 JSON 본문이면 400-2이다")
    @WithUserDetails("user1@test.com")
    void sendWithMalformedJson() throws Exception {
        long recipientId = member("user2@test.com").getId();

        ResultActions resultActions = mvc.perform(post("/api/v1/members/{memberId}/messages", recipientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"));
    }

    @Test
    @DisplayName("쪽지 발송: 내용이 1,000자를 초과하면 400-1이다")
    @WithUserDetails("user1@test.com")
    void sendWithTooLongContent() throws Exception {
        long recipientId = member("user2@test.com").getId();

        ResultActions resultActions = mvc.perform(post("/api/v1/members/{memberId}/messages", recipientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"content\": \"%s\" }".formatted("a".repeat(1001))));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("쪽지함 조회: 기본값은 받은 쪽지함이며 페이징된 요약 정보를 반환한다")
    @WithUserDetails("user1@test.com")
    void getReceivedMessages() throws Exception {
        saveProfile("user1@test.com", "user1");
        saveProfile("user2@test.com", "user2");
        Message message = saveMessage("user2@test.com", "user1@test.com", "받은 쪽지 내용입니다.");

        ResultActions resultActions = mvc.perform(get("/api/v1/members/me/messages")
                .param("page", "0")
                .param("size", "20"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("쪽지함 조회 성공"))
                .andExpect(jsonPath("$.data.content[0].id").value(message.getId()))
                .andExpect(jsonPath("$.data.content[0].sender.id").value(member("user2@test.com").getId()))
                .andExpect(jsonPath("$.data.content[0].sender.name").value(member("user2@test.com").getName()))
                .andExpect(jsonPath("$.data.content[0].sender.nickname").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].recipient.id").value(member("user1@test.com").getId()))
                .andExpect(jsonPath("$.data.content[0].recipient.name").value(member("user1@test.com").getName()))
                .andExpect(jsonPath("$.data.content[0].recipient.nickname").isNotEmpty())
                .andExpect(jsonPath("$.data.content[0].contentPreview").value("받은 쪽지 내용입니다."))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false))
                .andExpect(jsonPath("$.data.content[0].createAt").isNotEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("쪽지함 조회: box=SENT이면 보낸 쪽지함을 조회한다")
    @WithUserDetails("user1@test.com")
    void getSentMessages() throws Exception {
        Message message = saveMessage("user1@test.com", "user2@test.com", "보낸 쪽지 내용입니다.");

        ResultActions resultActions = mvc.perform(get("/api/v1/members/me/messages")
                .param("box", "SENT"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content[0].id").value(message.getId()))
                .andExpect(jsonPath("$.data.content[0].contentPreview").value("보낸 쪽지 내용입니다."));
    }

    @Test
    @DisplayName("쪽지함 조회: 프로필이 없으면 nickname은 null이다")
    @WithUserDetails("user1@test.com")
    void getMessagesWithoutProfile() throws Exception {
        saveMessage("user1@test.com", "user2@test.com", "프로필 없는 회원의 쪽지");

        ResultActions resultActions = mvc.perform(get("/api/v1/members/me/messages").param("box", "SENT"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].sender.nickname").isEmpty())
                .andExpect(jsonPath("$.data.content[0].recipient.nickname").isEmpty());
    }

    @Test
    @DisplayName("쪽지함 조회: 유효하지 않은 box 값이면 400-1이다")
    @WithUserDetails("user1@test.com")
    void getMessagesWithInvalidBox() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/members/me/messages")
                .param("box", "INVALID"));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("쪽지함 조회: page와 size 범위를 벗어나면 400-1이다")
    @WithUserDetails("user1@test.com")
    void getMessagesWithInvalidPageOrSize() throws Exception {
        mvc.perform(get("/api/v1/members/me/messages").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        mvc.perform(get("/api/v1/members/me/messages").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("쪽지함 조회: 로그인하지 않으면 401-1이다")
    void getMessagesWithoutLogin() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/members/me/messages"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("받은 쪽지 읽음 처리: 수신자가 요청한 모든 쪽지를 읽음으로 처리한다")
    @WithUserDetails("user1@test.com")
    void readReceivedMessages() throws Exception {
        Message first = saveMessage("user2@test.com", "user1@test.com", "첫 번째 쪽지");
        Message second = saveMessage("user2@test.com", "user1@test.com", "두 번째 쪽지");

        ResultActions resultActions = mvc.perform(patch("/api/v1/members/me/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "ids": [%d, %d] }
                        """.formatted(first.getId(), second.getId())));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("편지 읽음 처리 성공"))
                .andExpect(jsonPath("$.data[0].id").value(first.getId()))
                .andExpect(jsonPath("$.data[0].isRead").value(true))
                .andExpect(jsonPath("$.data[1].id").value(second.getId()))
                .andExpect(jsonPath("$.data[1].isRead").value(true));

        entityManager.flush();
        entityManager.clear();
        assertThat(messageRepository.findById(first.getId()).orElseThrow().isRead()).isTrue();
        assertThat(messageRepository.findById(second.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test
    @DisplayName("받은 쪽지 읽음 처리: ids가 비어 있으면 400-1이다")
    @WithUserDetails("user1@test.com")
    void readWithEmptyIds() throws Exception {
        ResultActions resultActions = mvc.perform(patch("/api/v1/members/me/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"ids\": [] }"));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("받은 쪽지 읽음 처리: 중복된 ids가 포함되면 400-1이다")
    @WithUserDetails("user1@test.com")
    void readWithDuplicateIds() throws Exception {
        Message message = saveMessage("user2@test.com", "user1@test.com", "중복 요청 쪽지");

        ResultActions resultActions = mvc.perform(patch("/api/v1/members/me/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"ids\": [%d, %d] }".formatted(message.getId(), message.getId())));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("받은 쪽지 읽음 처리: 로그인하지 않으면 401-1이다")
    void readWithoutLogin() throws Exception {
        ResultActions resultActions = mvc.perform(patch("/api/v1/members/me/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"ids\": [1] }"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }

    @Test
    @DisplayName("받은 쪽지 읽음 처리: 존재하지 않는 쪽지가 포함되면 404-1이다")
    @WithUserDetails("user1@test.com")
    void readWithNonexistentMessage() throws Exception {
        ResultActions resultActions = mvc.perform(patch("/api/v1/members/me/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"ids\": [999999] }"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("받은 쪽지 읽음 처리: 수신자가 아닌 쪽지를 포함하면 403-1이다")
    @WithUserDetails("user1@test.com")
    void readMessageNotReceivedByActor() throws Exception {
        Message message = saveMessage("user2@test.com", "admin", "다른 회원의 쪽지");

        ResultActions resultActions = mvc.perform(patch("/api/v1/members/me/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"ids\": [%d] }".formatted(message.getId())));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("본인과 관련된 쪽지만 조회/처리할 수 있습니다."));
    }

    @Test
    @DisplayName("쪽지 상세 조회: 수신자가 조회하면 읽음 처리된 상세를 반환한다")
    @WithUserDetails("user1@test.com")
    void getMessageDetail() throws Exception {
        saveProfile("user1@test.com", "user1");
        saveProfile("user2@test.com", "user2");
        Message message = saveMessage("user2@test.com", "user1@test.com", "쪽지 상세 내용입니다.");

        ResultActions resultActions = mvc.perform(get("/api/v1/members/me/messages/{messageId}", message.getId()));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("쪽지 상세 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(message.getId()))
                .andExpect(jsonPath("$.data.sender.id").value(member("user2@test.com").getId()))
                .andExpect(jsonPath("$.data.sender.name").value(member("user2@test.com").getName()))
                .andExpect(jsonPath("$.data.sender.nickname").isNotEmpty())
                .andExpect(jsonPath("$.data.recipient.id").value(member("user1@test.com").getId()))
                .andExpect(jsonPath("$.data.recipient.name").value(member("user1@test.com").getName()))
                .andExpect(jsonPath("$.data.recipient.nickname").isNotEmpty())
                .andExpect(jsonPath("$.data.content").value("쪽지 상세 내용입니다."))
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.createAt").isNotEmpty());
    }

    @Test
    @DisplayName("쪽지 상세 조회: 발신자 조회는 수신 읽음 상태를 변경하지 않는다")
    @WithUserDetails("user2@test.com")
    void getMessageDetailBySenderDoesNotMarkRead() throws Exception {
        Message message = saveMessage("user2@test.com", "user1@test.com", "보낸 쪽지 상세 내용입니다.");

        mvc.perform(get("/api/v1/members/me/messages/{messageId}", message.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(false));

        entityManager.flush();
        entityManager.clear();
        assertThat(messageRepository.findById(message.getId()).orElseThrow().isRead()).isFalse();
    }

    @Test
    @DisplayName("쪽지 상세 조회: 존재하지 않는 쪽지면 404-1이다")
    @WithUserDetails("user1@test.com")
    void getNonexistentMessageDetail() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/members/me/messages/999999"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 쪽지입니다."));
    }

    @Test
    @DisplayName("쪽지 상세 조회: 본인과 무관한 쪽지면 403-1이다")
    @WithUserDetails("user1@test.com")
    void getMessageDetailNotRelatedToActor() throws Exception {
        Message message = saveMessage("user2@test.com", "admin", "다른 회원의 쪽지");

        ResultActions resultActions = mvc.perform(get("/api/v1/members/me/messages/{messageId}", message.getId()));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("본인과 관련된 쪽지만 조회/처리할 수 있습니다."));
    }

    @Test
    @DisplayName("쪽지 상세 조회: 로그인하지 않으면 401-1이다")
    void getMessageDetailWithoutLogin() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/members/me/messages/1"));

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("401-1"));
    }
}
