package com.back.domain.party.showcase.comment.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.assemble.entity.PartyAssemble;
import com.back.domain.party.assemble.entity.PartyAssembleToMember;
import com.back.domain.party.assemble.repository.PartyAssembleRepository;
import com.back.domain.party.assemble.repository.PartyAssembleToMemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
import com.back.domain.party.showcase.comment.entity.ShowcaseComment;
import com.back.domain.party.showcase.comment.repository.ShowcaseCommentRepository;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1ShowcaseCommentControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyShowcaseRepository partyShowcaseRepository;

    @Autowired
    private PartyAssembleRepository partyAssembleRepository;

    @Autowired
    private PartyAssembleToMemberRepository partyAssembleToMemberRepository;

    @Autowired
    private ShowcaseCommentRepository showcaseCommentRepository;

    private Party savePublishedShowcase(String ownerEmail, String... assembledMemberEmails) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();

        Party party = new Party(
                owner,
                "오락실 팀",
                "오락실 공모전 팀원 모집",
                "설명",
                null,
                null,
                null,
                TopicType.PROJECT,
                PartyTag.WEB,
                "https://github.com/example/arcade",
                LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 2));
        party = partyRepository.save(party);

        PartyAssemble assemble = partyAssembleRepository.save(new PartyAssemble(party));
        partyAssembleToMemberRepository.save(new PartyAssembleToMember(assemble, owner));
        for (String email : assembledMemberEmails) {
            Member member = memberRepository.findByEmail(email).orElseThrow();
            partyAssembleToMemberRepository.save(new PartyAssembleToMember(assemble, member));
        }

        PartyShowcase showcase = new PartyShowcase(party);
        showcase.publish("정산 자동화 API", "설명");
        partyShowcaseRepository.save(showcase);

        return party;
    }

    private String writeRequestJson(String content, Long parentId) {
        return """
            {
                "content": "%s",
                "parentId": %s
            }
            """.formatted(content, parentId == null ? "null" : parentId);
    }

    @Test
    @DisplayName("댓글 작성: 파티원이 남기면 isPartyMember가 true다")
    @WithUserDetails("user1@test.com")
    void writeCommentAsPartyMember() throws Exception {
        Party party = savePublishedShowcase("user1@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/showcase/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("좋은 결과물이네요", null)));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.content").value("좋은 결과물이네요"))
                .andExpect(jsonPath("$.data.isPartyMember").value(true))
                .andExpect(jsonPath("$.data.deleted").value(false));
    }

    @Test
    @DisplayName("댓글 작성: 파티원이 아니면 isPartyMember가 false다")
    @WithUserDetails("user2@test.com")
    void writeCommentAsNonPartyMember() throws Exception {
        Party party = savePublishedShowcase("user1@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/showcase/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("축하드려요", null)));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPartyMember").value(false));
    }

    @Test
    @DisplayName("댓글 작성: 게시되지 않은 전시면 404-1이다")
    @WithUserDetails("user1@test.com")
    void writeCommentBeforePublish() throws Exception {
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Party party = partyRepository.save(new Party(
                owner, "미게시 팀", "미게시", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, "https://github.com/example/arcade",
                LocalDateTime.now().plusDays(7)
        ));

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/showcase/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("아직 게시 전", null)));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("대댓글 작성: 원댓글 하나에 여러 대댓글이 달리고, 목록 조회 시 트리로 묶인다")
    @WithUserDetails("user1@test.com")
    void repliesAreGroupedUnderParent() throws Exception {
        Party party = savePublishedShowcase("user1@test.com", "user2@test.com");
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElseThrow();
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member other = memberRepository.findByEmail("user2@test.com").orElseThrow();

        ShowcaseComment root = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, owner, null, "원댓글"));
        showcaseCommentRepository.save(new ShowcaseComment(showcase, other, root, "대댓글1"));
        showcaseCommentRepository.save(new ShowcaseComment(showcase, owner, root, "대댓글2"));

        ResultActions resultActions = mvc.perform(get("/api/v1/parties/" + party.getId() + "/showcase/comments"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].content").value("원댓글"))
                .andExpect(jsonPath("$.data[0].replies.length()").value(2))
                .andExpect(jsonPath("$.data[0].replies[0].content").value("대댓글1"))
                .andExpect(jsonPath("$.data[0].replies[1].content").value("대댓글2"));
    }

    @Test
    @DisplayName("대댓글 작성: 대댓글에 답글을 달면 400-1이다")
    @WithUserDetails("user1@test.com")
    void cannotReplyToAReply() throws Exception {
        Party party = savePublishedShowcase("user1@test.com");
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElseThrow();
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();

        ShowcaseComment root = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, owner, null, "원댓글"));
        ShowcaseComment reply = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, owner, root, "대댓글"));

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/showcase/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("대댓글에 또 답글", reply.getId())));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("댓글 수정: 작성자 본인이면 수정된다")
    @WithUserDetails("user1@test.com")
    void editByAuthor() throws Exception {
        Party party = savePublishedShowcase("user1@test.com");
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElseThrow();
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        ShowcaseComment comment = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, owner, null, "원본 내용"));

        ResultActions resultActions = mvc.perform(put(
                        "/api/v1/parties/" + party.getId() + "/showcase/comments/" + comment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("수정된 내용", null)));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("댓글 수정: 작성자도 관리자도 아니면 403-1이다")
    @WithUserDetails("user2@test.com")
    void editByNonAuthorFails() throws Exception {
        Party party = savePublishedShowcase("user1@test.com");
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElseThrow();
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        ShowcaseComment comment = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, owner, null, "원본 내용"));

        ResultActions resultActions = mvc.perform(put(
                        "/api/v1/parties/" + party.getId() + "/showcase/comments/" + comment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("몰래 수정", null)));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("댓글 수정: 작성자가 아니어도 관리자면 수정할 수 있다")
    @WithUserDetails("user2@test.com")
    void editByAdmin() throws Exception {
        Party party = savePublishedShowcase("user1@test.com");
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElseThrow();
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member admin = memberRepository.findByEmail("user2@test.com").orElseThrow();
        admin.grantAdmin();
        memberRepository.save(admin);
        ShowcaseComment comment = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, owner, null, "원본 내용"));

        ResultActions resultActions = mvc.perform(put(
                        "/api/v1/parties/" + party.getId() + "/showcase/comments/" + comment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequestJson("관리자가 수정", null)));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("댓글 삭제: 작성자 본인이 삭제하면 소프트 삭제되고 대댓글은 남는다")
    @WithUserDetails("user1@test.com")
    void deleteByAuthorKeepsReplies() throws Exception {
        Party party = savePublishedShowcase("user1@test.com", "user2@test.com");
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElseThrow();
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member other = memberRepository.findByEmail("user2@test.com").orElseThrow();
        ShowcaseComment root = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, owner, null, "원댓글"));
        showcaseCommentRepository.save(new ShowcaseComment(showcase, other, root, "대댓글"));

        mvc.perform(delete("/api/v1/parties/" + party.getId() + "/showcase/comments/" + root.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        ResultActions resultActions = mvc.perform(get("/api/v1/parties/" + party.getId() + "/showcase/comments"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].deleted").value(true))
                .andExpect(jsonPath("$.data[0].content").value("삭제된 댓글입니다."))
                .andExpect(jsonPath("$.data[0].replies.length()").value(1))
                .andExpect(jsonPath("$.data[0].replies[0].content").value("대댓글"));
    }

    @Test
    @DisplayName("댓글 삭제: 작성자가 아니어도 관리자면 삭제할 수 있다")
    @WithUserDetails("user2@test.com")
    void deleteByAdmin() throws Exception {
        Party party = savePublishedShowcase("user1@test.com");
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElseThrow();
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        Member admin = memberRepository.findByEmail("user2@test.com").orElseThrow();
        admin.grantAdmin();
        memberRepository.save(admin);
        ShowcaseComment comment = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, owner, null, "지워질 댓글"));

        mvc.perform(delete("/api/v1/parties/" + party.getId() + "/showcase/comments/" + comment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("댓글 삭제: 작성자도 관리자도 아니면 403-1이다")
    @WithUserDetails("user2@test.com")
    void deleteByNonAuthorNonAdminFails() throws Exception {
        Party party = savePublishedShowcase("user1@test.com");
        PartyShowcase showcase = partyShowcaseRepository.findByParty(party).orElseThrow();
        Member owner = memberRepository.findByEmail("user1@test.com").orElseThrow();
        ShowcaseComment comment = showcaseCommentRepository.save(
                new ShowcaseComment(showcase, owner, null, "지워지면 안됨"));

        mvc.perform(delete("/api/v1/parties/" + party.getId() + "/showcase/comments/" + comment.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }
}
