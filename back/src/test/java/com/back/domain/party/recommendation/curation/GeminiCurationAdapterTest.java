package com.back.domain.party.recommendation.curation;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiCurationAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();

    private GeminiCurationAdapter newAdapter(String apiKey) {
        GeminiCurationAdapter adapter = new GeminiCurationAdapter(builder, objectMapper);
        ReflectionTestUtils.setField(adapter, "model", "gemini-2.0-flash");
        ReflectionTestUtils.setField(adapter, "apiKey", apiKey);
        return adapter;
    }

    private MemberCurationContext context() {
        return new MemberCurationContext(PositionType.BACK, List.of("Spring", "Java"), List.of("백엔드 스터디"));
    }

    private List<PartyCandidate> candidates() {
        return List.of(
                new PartyCandidate(1L, "백엔드 스터디 모집", "함께 성장해요", TopicType.STUDY, PartyTag.WEB),
                new PartyCandidate(2L, "프론트 프로젝트", "리액트로 만들어요", TopicType.PROJECT, PartyTag.WEB)
        );
    }

    /** Gemini candidates[0].content.parts[0].text 안에 JSON 배열이 "문자열"로 한 번 더 인코딩되어 온다. */
    private String geminiResponseBody(String innerJsonArray) {
        String escapedInner = objectMapper.writeValueAsString(innerJsonArray);
        return """
                {"candidates": [{"content": {"parts": [{"text": %s}]}}]}
                """.formatted(escapedInner);
    }

    @Test
    @DisplayName("Gemini가 유효한 순위를 응답하면 그대로 반환한다")
    void curateReturnsGeminiRanking() {
        GeminiCurationAdapter adapter = newAdapter("test-key");
        String innerJsonArray = "[{\"partyId\":2,\"rank\":1,\"reason\":\"프론트 경험과 잘 맞아요\"},"
                + "{\"partyId\":1,\"rank\":2,\"reason\":\"기술 스택이 겹쳐요\"}]";

        mockServer.expect(requestTo(containsString(":generateContent")))
                .andExpect(method(POST))
                .andRespond(withSuccess(geminiResponseBody(innerJsonArray), MediaType.APPLICATION_JSON));

        List<CurationResult> results = adapter.curate(context(), candidates());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).partyId()).isEqualTo(2L);
        assertThat(results.get(0).rank()).isEqualTo(1);
        assertThat(results.get(0).reason()).isEqualTo("프론트 경험과 잘 맞아요");
        assertThat(results.get(1).partyId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("응답에 없는 후보 id는 버리고, 유효한 것만 반환한다")
    void curateDropsUnknownPartyIds() {
        GeminiCurationAdapter adapter = newAdapter("test-key");
        String innerJsonArray = "[{\"partyId\":999,\"rank\":1,\"reason\":\"지어낸 id\"},"
                + "{\"partyId\":1,\"rank\":2,\"reason\":\"실제 후보\"}]";

        mockServer.expect(requestTo(containsString(":generateContent")))
                .andExpect(method(POST))
                .andRespond(withSuccess(geminiResponseBody(innerJsonArray), MediaType.APPLICATION_JSON));

        List<CurationResult> results = adapter.curate(context(), candidates());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).partyId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Gemini 호출이 실패하면 후보 순서 그대로 폴백한다")
    void curateFallsBackOnServerError() {
        GeminiCurationAdapter adapter = newAdapter("test-key");

        mockServer.expect(requestTo(containsString(":generateContent")))
                .andExpect(method(POST))
                .andRespond(withServerError());

        List<CurationResult> results = adapter.curate(context(), candidates());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).partyId()).isEqualTo(1L);
        assertThat(results.get(0).reason()).isNull();
        assertThat(results.get(1).partyId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("API 키가 없으면 호출 없이 바로 폴백한다")
    void curateFallsBackWhenApiKeyMissing() {
        GeminiCurationAdapter adapter = newAdapter("");

        List<CurationResult> results = adapter.curate(context(), candidates());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).partyId()).isEqualTo(1L);
        mockServer.verify(); // expect() 설정이 없으므로 요청이 실제로 안 나갔어야 통과
    }

    @Test
    @DisplayName("후보가 비어있으면 호출 없이 빈 목록을 반환한다")
    void curateReturnsEmptyForNoCandidates() {
        GeminiCurationAdapter adapter = newAdapter("test-key");

        List<CurationResult> results = adapter.curate(context(), List.of());

        assertThat(results).isEmpty();
        mockServer.verify();
    }
}
