package com.back.domain.party.recommendation.curation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Gemini API로 후보 파티를 재정렬하고 추천 사유를 받아온다
// custom.curation.enabled=true일 때만 활성화되며, 호출 실패, 쿼터초과 시 PartyCurationPort.fallbackOrder로 폴백해 키워드 매칭 순서를 그대로 쓴다.
@Slf4j
@Component
@ConditionalOnProperty(name = "custom.curation.enabled", havingValue = "true")
public class GeminiCurationAdapter implements PartyCurationPort {

    private static final int REASON_MAX_LENGTH = 120;

    private final RestClient client;
    private final ObjectMapper objectMapper;

    @Value("${custom.curation.model:gemini-2.0-flash}")
    private String model;

    @Value("${custom.curation.gemini.api-key:}")
    private String apiKey;

    public GeminiCurationAdapter(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.client = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CurationResult> curate(MemberCurationContext memberContext, List<PartyCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        if (apiKey.isBlank()) {
            log.warn("Gemini API 키가 설정되지 않아 큐레이션을 건너뜁니다.");
            return PartyCurationPort.fallbackOrder(candidates);
        }

        try {
            JsonNode response = client.post()
                    .uri("/v1beta/models/{model}:generateContent?key={apiKey}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(memberContext, candidates))
                    .retrieve()
                    .body(JsonNode.class);

            List<CurationResult> results = parseResponse(response, candidates);
            if (results.isEmpty()) {
                log.warn("Gemini 응답에서 유효한 큐레이션 결과를 얻지 못해 폴백합니다.");
                return PartyCurationPort.fallbackOrder(candidates);
            }
            return results;
        } catch (Exception e) {
            log.warn("Gemini 큐레이션 호출 실패 - 키워드 매칭 순서로 폴백합니다.", e);
            return PartyCurationPort.fallbackOrder(candidates);
        }
    }

    private ObjectNode buildRequestBody(MemberCurationContext memberContext, List<PartyCandidate> candidates) {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", buildPrompt(memberContext, candidates));

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", buildResponseSchema());

        return root;
    }

    private String buildPrompt(MemberCurationContext memberContext, List<PartyCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("아래 회원에게 어울리는 팀 프로젝트(파티)를 후보 목록 중에서 골라 순위와 추천 이유를 매겨줘.\n\n");

        sb.append("[회원 정보]\n");
        sb.append("포지션: ").append(memberContext.position() == null ? "미설정" : memberContext.position()).append("\n");
        sb.append("기술 스택: ").append(String.join(", ", memberContext.techStacks())).append("\n");
        sb.append("최근 검색어: ").append(String.join(", ", memberContext.recentSearchKeywords())).append("\n\n");

        sb.append("[후보 파티 목록]\n");
        for (PartyCandidate candidate : candidates) {
            sb.append("- id: ").append(candidate.partyId())
                    .append(", 제목: ").append(candidate.title())
                    .append(", 주제: ").append(candidate.topicType())
                    .append(", 태그: ").append(candidate.partyTag())
                    .append(", 설명: ").append(truncate(candidate.description(), 200))
                    .append("\n");
        }

        sb.append("\n반드시 위 후보 목록에 있는 id만 사용하고, 최대 ").append(candidates.size()).append("개까지 순위를 매겨줘. ")
                .append("reason은 한국어로 ").append(REASON_MAX_LENGTH).append("자 이내로 짧게 써줘.");

        return sb.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private ObjectNode buildResponseSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "ARRAY");
        ObjectNode items = schema.putObject("items");
        items.put("type", "OBJECT");
        ObjectNode properties = items.putObject("properties");
        properties.putObject("partyId").put("type", "INTEGER");
        properties.putObject("rank").put("type", "INTEGER");
        properties.putObject("reason").put("type", "STRING");
        ArrayNode required = items.putArray("required");
        required.add("partyId");
        required.add("rank");
        required.add("reason");
        return schema;
    }

    private List<CurationResult> parseResponse(JsonNode response, List<PartyCandidate> candidates) {
        if (response == null) {
            return List.of();
        }
        String rawText = response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asString();
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        JsonNode parsed = objectMapper.readTree(rawText);
        if (!parsed.isArray()) {
            return List.of();
        }

        Set<Long> validPartyIds = candidates.stream().map(PartyCandidate::partyId).collect(Collectors.toSet());

        List<CurationResult> results = new ArrayList<>();
        for (JsonNode item : parsed) {
            long partyId = item.path("partyId").asLong();
            if (!validPartyIds.contains(partyId)) {
                continue; // 후보에 없는 id를 지어내면 버린다.
            }
            int rank = item.path("rank").asInt();
            String reason = truncate(item.path("reason").asString(""), REASON_MAX_LENGTH);
            results.add(new CurationResult(partyId, rank, reason));
        }

        return results.stream()
                .sorted((a, b) -> Integer.compare(a.rank(), b.rank()))
                .toList();
    }
}
