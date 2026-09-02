package com.back.domain.party.showcase.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1PartyShowcaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PartyShowcaseRepository partyShowcaseRepository;

    private Party saveParty(String ownerEmail) {
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
                1,
                LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, 2));

        return partyRepository.save(party);
    }

    private String publishRequestJson(String title, String description) {
        return """
            {
                "title": "%s",
                "description": "%s"
            }
            """.formatted(title, description);
    }

    @Test
    @DisplayName("전시 초안 조회: 게시 전이면 title/description은 null, published는 false다")
    @WithUserDetails("user1@test.com")
    void getDraftBeforePublish() throws Exception {
        Party party = saveParty("user1@test.com");

        ResultActions resultActions = mvc.perform(get("/api/v1/parties/" + party.getId() + "/showcase"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.partyId").value(party.getId()))
                .andExpect(jsonPath("$.data.partyName").value("오락실 팀"))
                .andExpect(jsonPath("$.data.ownerName").isNotEmpty())
                .andExpect(jsonPath("$.data.githubRepoUrl").value("https://github.com/example/arcade"))
                .andExpect(jsonPath("$.data.title").doesNotExist())
                .andExpect(jsonPath("$.data.published").value(false));
    }

    @Test
    @DisplayName("전시 초안 조회: 존재하지 않는 파티면 404-1이다")
    @WithUserDetails("user1@test.com")
    void getDraftPartyNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/parties/999999/showcase"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("전시 게시: 201-1과 함께 published가 true로 전환된다")
    @WithUserDetails("user1@test.com")
    void publish() throws Exception {
        Party party = saveParty("user1@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/showcase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishRequestJson("정산 자동화 API", "정산 배치와 캐시 레이어를 담당했습니다.")));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.title").value("정산 자동화 API"))
                .andExpect(jsonPath("$.data.description").value("정산 배치와 캐시 레이어를 담당했습니다."))
                .andExpect(jsonPath("$.data.published").value(true));
    }

    @Test
    @DisplayName("전시 게시: 재게시하면 기존 글이 수정된다(upsert)")
    @WithUserDetails("user1@test.com")
    void republishUpdatesExisting() throws Exception {
        Party party = saveParty("user1@test.com");

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/showcase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishRequestJson("첫 번째 제목", "첫 번째 설명")))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/showcase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishRequestJson("수정된 제목", "수정된 설명")));

        resultActions.andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                .andExpect(jsonPath("$.data.description").value("수정된 설명"));

        // PartyShowcase 행이 새로 늘어난 게 아니라 그대로 하나만 있어야 한다
        long showcaseCount = partyShowcaseRepository.findByParty(party).stream().count();
        org.assertj.core.api.Assertions.assertThat(showcaseCount).isEqualTo(1);
    }

    @Test
    @DisplayName("전시 게시: 파티장이 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void publishAsNonOwner() throws Exception {
        Party party = saveParty("user2@test.com");

        ResultActions resultActions = mvc.perform(post("/api/v1/parties/" + party.getId() + "/showcase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishRequestJson("제목", "설명")));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("전시 게시: 존재하지 않는 파티면 404-1이다")
    @WithUserDetails("user1@test.com")
    void publishPartyNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/parties/999999/showcase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishRequestJson("제목", "설명")));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("게시 후 초안 조회하면 저장된 내용이 그대로 반환된다")
    @WithUserDetails("user1@test.com")
    void getDraftAfterPublish() throws Exception {
        Party party = saveParty("user1@test.com");

        mvc.perform(post("/api/v1/parties/" + party.getId() + "/showcase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishRequestJson("정산 자동화 API", "설명")))
                .andExpect(status().isCreated());

        ResultActions resultActions = mvc.perform(get("/api/v1/parties/" + party.getId() + "/showcase"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("정산 자동화 API"))
                .andExpect(jsonPath("$.data.published").value(true))
                .andExpect(jsonPath("$.data.publishedAt").exists());
    }

    @Test
    @DisplayName("인기 전시회 TOP3: 로그인하지 않아도 조회할 수 있다")
    void getTop3WithoutLogin() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/parties/showcase/top3"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("인기 전시회 TOP3: 게시되지 않은 전시는 포함되지 않는다")
    @WithUserDetails("user1@test.com")
    void getTop3ExcludesUnpublished() throws Exception {
        Party party = saveParty("user1@test.com");
        // 게시하지 않고 그대로 둠

        ResultActions resultActions = mvc.perform(get("/api/v1/parties/showcase/top3"));

        resultActions.andExpect(status().isOk());
        resultActions.andExpect(jsonPath("$.data[?(@.partyId == " + party.getId() + ")]").doesNotExist());
    }

    @Test
    @DisplayName("인기 전시회 TOP3: 게시된 전시는 좋아요 수 내림차순으로 정렬된다")
    @WithUserDetails("user1@test.com")
    void getTop3OrderedByLikeCount() throws Exception {
        Party lowLikeParty = saveParty("user1@test.com");
        mvc.perform(post("/api/v1/parties/" + lowLikeParty.getId() + "/showcase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishRequestJson("좋아요 적음", "설명")))
                .andExpect(status().isCreated());

        Party highLikeParty = saveParty("user1@test.com");
        mvc.perform(post("/api/v1/parties/" + highLikeParty.getId() + "/showcase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publishRequestJson("좋아요 많음", "설명")))
                .andExpect(status().isCreated());

        partyRepository.increaseLikeCount(highLikeParty.getId());
        partyRepository.increaseLikeCount(highLikeParty.getId());
        partyRepository.increaseLikeCount(lowLikeParty.getId());

        ResultActions resultActions = mvc.perform(get("/api/v1/parties/showcase/top3"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].partyId").value(highLikeParty.getId()));
    }
}
