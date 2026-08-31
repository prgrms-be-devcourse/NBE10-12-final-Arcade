package com.back.global.github.client;

import com.back.global.github.client.dto.GithubPullRequestResponse;
import com.back.global.exception.ServiceException;
import tools.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** GitHub App JWT와 짧게 사는 installation token으로만 GitHub REST API를 호출한다. */
@Component
public class GithubAppClient {
    private final RestClient client = RestClient.builder().baseUrl("https://api.github.com").build();

    @Value("${custom.github.app.id:}")
    private String appId;
    @Value("${custom.github.app.privateKey:}")
    private String privateKeyPem;

    public String createInstallationToken(long installationId) {
        JsonNode response = appHeaders(
                client.post()
                        .uri("/app/installations/{id}/access_tokens", installationId))
            .retrieve().body(JsonNode.class);
        String token = response == null ? null : response.path("token").asText();
        if (token == null || token.isBlank()) throw new ServiceException("502-20", "GITHUB_INSTALLATION_TOKEN_CREATE_FAILED");
        return token;
    }

    public Repository findRepository(String installationToken, String expectedFullName) {
        for (int page = 1; ; page++) {
            JsonNode response = tokenHeaders(client.get().uri("/installation/repositories?per_page=100&page={page}", page), installationToken)
                .retrieve().body(JsonNode.class);
            JsonNode repositories = response == null ? null : response.path("repositories");
            if (repositories == null || !repositories.isArray() || repositories.isEmpty()) break;
            for (JsonNode repository : repositories) {
                if (expectedFullName.equalsIgnoreCase(repository.path("full_name").asText())) {
                    return new Repository(repository.path("id").asLong(), repository.path("full_name").asText());
                }
            }
            if (repositories.size() < 100) break;
        }
        throw new ServiceException("403-20", "GITHUB_APP_INSTALLATION_REPOSITORY_NOT_SELECTED");
    }

    public List<GithubPullRequestResponse> getAllPullRequests(String installationToken, String repository) {
        String[] repositoryPath = repository.split("/", -1);
        if (repositoryPath.length != 2 || repositoryPath[0].isBlank() || repositoryPath[1].isBlank()) {
            throw new IllegalArgumentException("GitHub repository must be in owner/repository format.");
        }
        String owner = repositoryPath[0];
        String name = repositoryPath[1];
        List<GithubPullRequestResponse> all = new ArrayList<>();
        for (int page = 1; ; page++) {
            GithubPullRequestResponse[] response = tokenHeaders(client.get()
                    .uri("/repos/{owner}/{repo}/pulls?state=all&per_page=100&page={page}", owner, name, page), installationToken)
                .retrieve().body(GithubPullRequestResponse[].class);
            if (response == null || response.length == 0) break;
            all.addAll(Arrays.asList(response));
            if (response.length < 100) break;
        }
        return all;
    }

    private RestClient.RequestHeadersSpec<?> appHeaders(RestClient.RequestHeadersSpec<?> request) {
        return headers(request, createAppJwt());
    }

    private RestClient.RequestHeadersSpec<?> tokenHeaders(RestClient.RequestHeadersSpec<?> request, String token) {
        return headers(request, token);
    }

    private RestClient.RequestHeadersSpec<?> headers(RestClient.RequestHeadersSpec<?> request, String token) {
        return request.header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28");
    }

    private String createAppJwt() {
        if (appId.isBlank() || privateKeyPem.isBlank()) throw new IllegalStateException("GitHub App ID 또는 private key가 설정되지 않았습니다.");
        Instant now = Instant.now();
        return Jwts.builder().issuedAt(java.util.Date.from(now.minusSeconds(30))).expiration(java.util.Date.from(now.plusSeconds(540)))
            .issuer(appId).signWith(privateKey()).compact();
    }

    private PrivateKey privateKey() {
        try {
            String value = privateKeyPem.replace("\\n", "\n").replaceAll("-----BEGIN (?:RSA )?PRIVATE KEY-----|-----END (?:RSA )?PRIVATE KEY-----|\\s", "");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(value)));
        } catch (Exception e) {
            throw new IllegalStateException("GitHub App private key를 읽을 수 없습니다.", e);
        }
    }

    public record Repository(long id, String fullName) {}
}
