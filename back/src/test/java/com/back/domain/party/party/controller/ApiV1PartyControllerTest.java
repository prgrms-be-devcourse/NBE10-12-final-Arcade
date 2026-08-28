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
}
