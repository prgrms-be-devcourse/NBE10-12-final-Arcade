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
}
