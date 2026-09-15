package com.back.domain.party.github.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

/** Party의 표시용 GitHub 레포 URL 저장은 App 설치·사용자 인증·PR binding과 독립적으로 처리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartyGithubRepositoryUrlService {
    private final PartyRepository partyRepository;

    @Transactional
    public String update(long partyId, String githubRepoUrl, Member actor) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "파티를 찾을 수 없습니다."));
        if (actor == null || !party.isOwnedBy(actor)) throw new ServiceException("403-1", "파티장만 GitHub 저장소 URL을 수정할 수 있습니다.");
        String normalized = normalize(githubRepoUrl);
        party.updateGithubRepoUrl(normalized);
        return normalized;
    }

    private String normalize(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())) throw new IllegalArgumentException();
            String[] segments = uri.getPath().replaceFirst("^/", "").replaceFirst("/+$", "").split("/");
            if (segments.length != 2 || segments[0].isBlank() || segments[1].isBlank()) throw new IllegalArgumentException();
            return "https://github.com/" + segments[0] + "/" + segments[1].replaceFirst("\\.git$", "");
        } catch (RuntimeException e) {
            throw new ServiceException("400-20", "GITHUB_REPOSITORY_URL_INVALID");
        }
    }
}
