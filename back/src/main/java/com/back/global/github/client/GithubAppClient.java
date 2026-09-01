package com.back.global.github.client;

import com.back.global.github.client.dtos.GithubPullRequestResponse;
import com.back.global.exception.ServiceException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

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
        String token = response == null ? null : response.path("token").asString();
        if (token == null || token.isBlank()) throw new ServiceException("502-20", "GITHUB_INSTALLATION_TOKEN_CREATE_FAILED");
        return token;
    }

    public Repository findRepository(String installationToken, String expectedFullName) {
        for (int page = 1; ; page++) {
            JsonNode response =
                    tokenHeaders(
                            client.get()
                                    .uri("/installation/repositories?per_page=100&page={page}", page),
                            installationToken
                    )
                            .retrieve()
                            .body(JsonNode.class);

            JsonNode repositories = response == null ? null : response.path("repositories");
            if (repositories == null || !repositories.isArray() || repositories.isEmpty()) break;
            for (JsonNode repository : repositories) {
                if (expectedFullName.equalsIgnoreCase(
                        repository.path("full_name").asString())) {
                    return new Repository(
                            repository.path("id").asLong(),
                            repository.path("full_name").asString()
                    );
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
            GithubPullRequestResponse[] response =
                    tokenHeaders(
                            client.get()
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
        if (appId.isBlank() || privateKeyPem.isBlank())
            throw new IllegalStateException("GitHub App ID 또는 private key가 설정되지 않았습니다.");

        Instant now = Instant.now();

        return Jwts.builder()
                .issuedAt(Date.from(now.minusSeconds(30)))
                .expiration(Date.from(now.plusSeconds(540)))
                .issuer(appId).signWith(privateKey()).compact();
    }

    private PrivateKey privateKey() {
        try {
            boolean pkcs1 = privateKeyPem.contains("-----BEGIN RSA ")
                    && privateKeyPem.contains("PRIVATE KEY-----");
            String value = privateKeyPem
                    .replace("\\n", "\n")
                    .replaceAll("-----BEGIN (?:RSA )?PRIVATE KEY-----|-----END (?:RSA )?PRIVATE KEY-----|\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(value);

            return KeyFactory.getInstance("RSA")
                    .generatePrivate(
                            new PKCS8EncodedKeySpec(
                                    pkcs1 ? pkcs1ToPkcs8(keyBytes) : keyBytes
                            )
                    );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "GitHub App private key를 읽을 수 없습니다.", e);
        }
    }

    /** PKCS#1 RSA 개인키를 PKCS#8 PrivateKeyInfo DER 구조로 감싼다. */
    private byte[] pkcs1ToPkcs8(byte[] pkcs1) {
        byte[] version = {0x02, 0x01, 0x00};
        byte[] rsaAlgorithmIdentifier = {
                0x30, 0x0D,
                0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        byte[] privateKey = derValue((byte) 0x04, pkcs1);
        byte[] body = new byte[version.length + rsaAlgorithmIdentifier.length + privateKey.length];

        System.arraycopy(version, 0, body, 0, version.length);
        System.arraycopy(rsaAlgorithmIdentifier, 0, body, version.length, rsaAlgorithmIdentifier.length);
        System.arraycopy(privateKey, 0, body, version.length + rsaAlgorithmIdentifier.length, privateKey.length);

        return derValue((byte) 0x30, body);
    }

    private byte[] derValue(byte tag, byte[] value) {
        byte[] length = derLength(value.length);
        byte[] result = new byte[1 + length.length + value.length];

        result[0] = tag;
        System.arraycopy(length, 0, result, 1, length.length);
        System.arraycopy(value, 0, result, 1 + length.length, value.length);

        return result;
    }

    private byte[] derLength(int length) {
        if (length < 128) return new byte[]{(byte) length};

        int bytes = 0;
        for (int remaining = length; remaining > 0; remaining >>>= 8) bytes++;

        byte[] result = new byte[bytes + 1];
        result[0] = (byte) (0x80 | bytes);
        for (int index = bytes; index > 0; index--) {
            result[index] = (byte) (length & 0xFF);
            length >>>= 8;
        }

        return result;
    }

    public record Repository(long id, String fullName) {}
}
