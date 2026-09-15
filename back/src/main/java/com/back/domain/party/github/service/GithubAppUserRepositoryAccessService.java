package com.back.domain.party.github.service;

import com.back.global.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** GitHub App user token의 installation/레포 접근 범위와 대상 레포 admin 권한을 확인한다. */
@Service
public class GithubAppUserRepositoryAccessService {
    private final RestClient client = RestClient.builder().baseUrl("https://api.github.com").build();

    public List<AccessibleRepository> repositories(String userAccessToken) {
        List<AccessibleRepository> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            JsonNode installations = request(userAccessToken, "/user/installations?per_page=100&page=" + page);
            JsonNode values = installations == null ? null : installations.path("installations");
            if (values == null || !values.isArray() || values.isEmpty()) break;
            for (JsonNode installation : values) {
                long installationId = installation.path("id").asLong();
                if (installationId > 0) result.addAll(repositories(userAccessToken, installationId));
            }
            if (values.size() < 100) break;
        }
        return result;
    }

    public boolean hasAdminPermission(String userAccessToken, String fullName) {
        JsonNode response = request(userAccessToken, "/repos/" + fullName);
        return response != null && response.path("permissions").path("admin").asBoolean(false);
    }

    private List<AccessibleRepository> repositories(String token, long installationId) {
        List<AccessibleRepository> result = new ArrayList<>();
        for (int page = 1; ; page++) {
            JsonNode response = request(token, "/user/installations/" + installationId
                    + "/repositories?per_page=100&page=" + page);
            JsonNode repositories = response == null ? null : response.path("repositories");
            if (repositories == null || !repositories.isArray() || repositories.isEmpty()) break;
            for (JsonNode repository : repositories) {
                long id = repository.path("id").asLong();
                String fullName = repository.path("full_name").asString();
                if (id > 0 && !fullName.isBlank()) result.add(new AccessibleRepository(installationId, id, fullName));
            }
            if (repositories.size() < 100) break;
        }
        return result;
    }

    private JsonNode request(String token, String uri) {
        try {
            return client.get().uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve().body(JsonNode.class);
        } catch (RuntimeException e) {
            throw new ServiceException("403-20", "GITHUB_APP_USER_REPOSITORY_ACCESS_DENIED");
        }
    }

    public record AccessibleRepository(long installationId, long repositoryId, String fullName) {
    }
}
