package com.back.domain.party.party.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.position.entity.Position;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiV1PartyControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PartyRepository partyRepository;

    private final String deadline = LocalDateTime.now().plusDays(7).toString();

    private String createPartyRequestJson(int backCapacity) {
        return """
            {
                "partyName": "오락실 팀",
                "title": "오락실 공모전 팀원 모집",
                "description": "설명",
                "topicType": "PROJECT",
                "partyTag": "WEB",
                "checklistRequiredApprovals": 1,
                "deadline": "%s",
                "positions": [
                    { "name": "BACK", "capacity": %d }
                ]
            }
            """.formatted(deadline, backCapacity);
    }

    @Test
    @DisplayName("파티 생성: 201-1과 생성된 파티를 반환한다")
    @WithUserDetails("user1@test.com")
    void createParty() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/parties")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPartyRequestJson(2)));

        resultActions.andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultCode").value("201-1"))
            .andExpect(jsonPath("$.msg").value("파티 생성 성공"))
            .andExpect(jsonPath("$.data.id").isNumber())
            .andExpect(jsonPath("$.data.partyName").value("오락실 팀"))
            .andExpect(jsonPath("$.data.status").value("RECRUITING"))
            .andExpect(jsonPath("$.data.positions[0].type").value("BACK"))
            .andExpect(jsonPath("$.data.positions[0].capacity").value(2));
    }

    @Test
    @DisplayName("파티 생성: 포지션 정원이 0 이하면 400-4이다")
    @WithUserDetails("user1@test.com")
    void createPartyWithInvalidCapacity() throws Exception {
        ResultActions resultActions = mvc.perform(post("/api/v1/parties")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createPartyRequestJson(0)));

        resultActions.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value("400-4"));
    }

    @Test
    @DisplayName("파티 생성: 포지션 목록이 비어있으면 400-1이다")
    @WithUserDetails("user1@test.com")
    void createPartyWithoutPositions() throws Exception {
        String body = """
            {
                "partyName": "오락실 팀",
                "title": "오락실 공모전 팀원 모집",
                "topicType": "PROJECT",
                "partyTag": "WEB",
                "checklistRequiredApprovals": 1,
                "deadline": "%s",
                "positions": []
            }
            """.formatted(deadline);

        ResultActions resultActions = mvc.perform(post("/api/v1/parties")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));

        resultActions.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.resultCode").value("400-1"));
    }
    private Party savePartyOwnedBy(String ownerEmail, int capacity) {
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
                null,
                1,
                LocalDateTime.now().plusDays(7)
        );
        party.addPosition(new Position(PositionType.BACK, capacity));

        return partyRepository.save(party);
    }

    @Test
    @DisplayName("파티 수정: 파티장이 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void updatePartyByNonOwner() throws Exception {
        Party party = savePartyOwnedBy("user2@test.com", 2);

        String updateBody = """
            {
                "partyName": "수정된 이름",
                "title": "수정된 제목",
                "topicType": "PROJECT",
                "partyTag": "WEB",
                "deadline": "%s"
            }
            """.formatted(deadline);

        ResultActions resultActions = mvc.perform(patch("/api/v1/parties/" + party.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("파티 수정: 정원을 승인 인원보다 작게 줄이면 400-4이다")
    @WithUserDetails("user1@test.com")
    void updatePartyWithCapacityBelowFilledCount() throws Exception {
        Party party = savePartyOwnedBy("user1@test.com", 3);
        Position position = party.getPositions().get(0);
        position.fillOneSeat();
        position.fillOneSeat();
        partyRepository.save(party); // filledCount == 2

        String updateBody = """
            {
                "partyName": "수정된 이름",
                "title": "수정된 제목",
                "topicType": "PROJECT",
                "partyTag": "WEB",
                "deadline": "%s",
                "positions": [
                    { "positionId": %d, "capacity": 1 }
                ]
            }
            """.formatted(deadline, position.getId());

        ResultActions resultActions = mvc.perform(patch("/api/v1/parties/" + party.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));
    }

    @Test
    @DisplayName("파티 수정: 정원을 0 이하로 바꾸면 400-4이다")
    @WithUserDetails("user1@test.com")
    void updatePartyWithNonPositiveCapacity() throws Exception {
        Party party = savePartyOwnedBy("user1@test.com", 2);
        Position position = party.getPositions().get(0);

        String updateBody = """
        {
            "partyName": "수정된 이름",
            "title": "수정된 제목",
            "topicType": "PROJECT",
            "partyTag": "WEB",
            "deadline": "%s",
            "positions": [
                { "positionId": %d, "capacity": 0 }
            ]
        }
        """.formatted(deadline, position.getId());

        ResultActions resultActions = mvc.perform(patch("/api/v1/parties/" + party.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody));

        resultActions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));
    }

    @Test
    @DisplayName("파티 삭제: 모집 중인 파티는 정상 삭제된다")
    @WithUserDetails("user1@test.com")
    void deleteRecruitingParty() throws Exception {
        Party party = savePartyOwnedBy("user1@test.com", 2);

        ResultActions resultActions = mvc.perform(delete("/api/v1/parties/" + party.getId()));

        resultActions.andExpect(status().isNoContent())
                .andExpect(jsonPath("$.resultCode").value("204-1"));
    }

    @Test
    @DisplayName("파티 삭제: 파티장이 아니면 403-1이다")
    @WithUserDetails("user1@test.com")
    void deletePartyByNonOwner() throws Exception {
        Party party = savePartyOwnedBy("user2@test.com", 2);

        ResultActions resultActions = mvc.perform(delete("/api/v1/parties/" + party.getId()));

        resultActions.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    private Party savePartyOwnedBy(
            String ownerEmail,
            String partyName,
            PositionType positionType,
            int capacity,
            PartyTag partyTag,
            LocalDateTime deadlineAt
    ) {
        Member owner = memberRepository.findByEmail(ownerEmail).orElseThrow();

        Party party = new Party(
                owner,
                partyName,
                partyName + " 모집",
                "설명",
                null,
                null,
                null,
                TopicType.PROJECT,
                partyTag,
                null,
                1,
                deadlineAt
        );
        party.addPosition(new Position(positionType, capacity));

        return partyRepository.save(party);
    }

    @Test
    @DisplayName("파티 목록 조회: 키워드로 파티명을 검색한다")
    @WithUserDetails("user1@test.com")
    void listPartiesByKeyword() throws Exception {
        savePartyOwnedBy("user1@test.com", "스프링 백엔드 스터디", PositionType.BACK, 2, PartyTag.WEB, LocalDateTime.now().plusDays(3));
        savePartyOwnedBy("user1@test.com", "리액트 프론트 스터디", PositionType.FRONT, 2, PartyTag.WEB, LocalDateTime.now().plusDays(3));

        ResultActions resultActions = mvc.perform(get("/api/v1/parties")
                .param("keyword", "스프링"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].partyName").value("스프링 백엔드 스터디"));
    }

    @Test
    @DisplayName("파티 목록 조회: 포지션으로 필터링한다")
    @WithUserDetails("user1@test.com")
    void listPartiesByPosition() throws Exception {
        savePartyOwnedBy("user1@test.com", "백엔드 파티", PositionType.BACK, 2, PartyTag.WEB, LocalDateTime.now().plusDays(3));
        savePartyOwnedBy("user1@test.com", "프론트 파티", PositionType.FRONT, 2, PartyTag.WEB, LocalDateTime.now().plusDays(3));

        ResultActions resultActions = mvc.perform(get("/api/v1/parties")
                .param("position", "FRONT"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].partyName").value("프론트 파티"));
    }

    @Test
    @DisplayName("파티 목록 조회: 분야(partyTag)로 필터링한다")
    @WithUserDetails("user1@test.com")
    void listPartiesByPartyTag() throws Exception {
        savePartyOwnedBy("user1@test.com", "웹 파티", PositionType.BACK, 2, PartyTag.WEB, LocalDateTime.now().plusDays(3));
        savePartyOwnedBy("user1@test.com", "게임 파티", PositionType.BACK, 2, PartyTag.GAME, LocalDateTime.now().plusDays(3));

        ResultActions resultActions = mvc.perform(get("/api/v1/parties")
                .param("partyTag", "GAME"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].partyName").value("게임 파티"));
    }

    @Test
    @DisplayName("파티 목록 조회: 기본 정렬(마감임박순)은 deadline 오름차순이다")
    @WithUserDetails("user1@test.com")
    void listPartiesSortedByDeadline() throws Exception {
        savePartyOwnedBy("user1@test.com", "늦게 마감", PositionType.BACK, 2, PartyTag.WEB, LocalDateTime.now().plusDays(10));
        savePartyOwnedBy("user1@test.com", "빨리 마감", PositionType.BACK, 2, PartyTag.WEB, LocalDateTime.now().plusDays(1));

        ResultActions resultActions = mvc.perform(get("/api/v1/parties")
                .param("sort", "DEADLINE"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].partyName").value("빨리 마감"))
                .andExpect(jsonPath("$.data.content[1].partyName").value("늦게 마감"));
    }

    @Test
    @DisplayName("파티 목록 조회: 빈자리순 정렬은 남은 정원 합이 큰 순이다")
    @WithUserDetails("user1@test.com")
    void listPartiesSortedByVacancy() throws Exception {
        // 정원 5에 1명 승인 -> 빈자리 4
        Party fewVacancy = savePartyOwnedBy("user1@test.com", "빈자리 적음", PositionType.BACK, 5, PartyTag.WEB, LocalDateTime.now().plusDays(3));
        fewVacancy.getPositions().get(0).fillOneSeat();
        partyRepository.save(fewVacancy);

        // 정원 5에 0명 승인 -> 빈자리 5
        savePartyOwnedBy("user1@test.com", "빈자리 많음", PositionType.BACK, 5, PartyTag.WEB, LocalDateTime.now().plusDays(3));

        ResultActions resultActions = mvc.perform(get("/api/v1/parties")
                .param("sort", "VACANCY"));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].partyName").value("빈자리 많음"))
                .andExpect(jsonPath("$.data.content[1].partyName").value("빈자리 적음"));
    }

    @Test
    @DisplayName("파티 상세 조회: 200과 D-day를 포함한 파티 정보를 반환한다")
    @WithUserDetails("user1@test.com")
    void getPartyDetail() throws Exception {
        Party party = savePartyOwnedBy("user1@test.com", 2);

        ResultActions resultActions = mvc.perform(get("/api/v1/parties/" + party.getId()));

        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.id").value(party.getId()))
                .andExpect(jsonPath("$.data.ownerName").isNotEmpty())
                .andExpect(jsonPath("$.data.dDay").isNumber())
                .andExpect(jsonPath("$.data.positions[0].type").value("BACK"));
    }

    @Test
    @DisplayName("파티 상세 조회: 조회할 때마다 viewCount가 증가한다")
    @WithUserDetails("user1@test.com")
    void getPartyDetailIncreasesViewCount() throws Exception {
        Party party = savePartyOwnedBy("user1@test.com", 2);

        mvc.perform(get("/api/v1/parties/" + party.getId()))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mvc.perform(get("/api/v1/parties/" + party.getId()))
                .andExpect(jsonPath("$.data.viewCount").value(2));
    }

    @Test
    @DisplayName("파티 상세 조회: 존재하지 않는 파티는 404-1이다")
    @WithUserDetails("user1@test.com")
    void getPartyDetailNotFound() throws Exception {
        ResultActions resultActions = mvc.perform(get("/api/v1/parties/999999"));

        resultActions.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }
}
