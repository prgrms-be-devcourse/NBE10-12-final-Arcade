package com.back.domain.party.github.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.party.github.entity.*;
import com.back.domain.party.github.repository.*;
import com.back.domain.party.party.entity.*;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.global.exception.ServiceException;
import com.back.global.github.client.GithubAppClient;
import com.back.global.github.event.GithubInstallationRepositoryRemovedEvent;
import com.back.global.github.event.GithubInstallationUnavailableEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PartyGithubBindingRegressionTest {
    @Autowired PartyGithubBindingService bindings;
    @Autowired PartyGithubConnectionService connections;
    @Autowired GithubInstallationInventoryService inventory;
    @Autowired PartyRepository parties;
    @Autowired MemberRepository members;
    @Autowired GithubAppInstallationRepository installations;
    @Autowired GithubInstallationRepositoryRepository repositories;
    @Autowired PartyGithubBindingRepository bindingRepository;
    @Autowired PartyGithubBindingAuditRepository audits;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean GithubAppUserAuthorizationService authorization;
    @MockitoBean GithubAppUserRepositoryAccessService access;
    @MockitoBean GithubAppClient github;
    private static final AtomicLong IDS = new AtomicLong(900000);

    private Fixture fixture() {
        Member owner = members.findByEmail("user1@test.com").orElseThrow();
        Party party = parties.save(new Party(owner, "연결 테스트", "테스트", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, null, 1, LocalDateTime.now().plusDays(7)));
        GithubAppInstallation installation = installations.save(new GithubAppInstallation(
                IDS.incrementAndGet(), 123L, "org", "Organization", null));
        GithubInstallationRepository first = repositories.save(new GithubInstallationRepository(installation, IDS.incrementAndGet(), "org/first"));
        GithubInstallationRepository second = repositories.save(new GithubInstallationRepository(installation, IDS.incrementAndGet(), "org/second"));
        when(authorization.validAccessToken(any())).thenReturn("user-token");
        when(access.repositories(anyString())).thenReturn(List.of(
                new GithubAppUserRepositoryAccessService.AccessibleRepository(installation.getInstallationId(), first.getRepositoryId(), first.getFullName()),
                new GithubAppUserRepositoryAccessService.AccessibleRepository(installation.getInstallationId(), second.getRepositoryId(), second.getFullName())));
        when(github.createInstallationToken(anyLong())).thenReturn("installation-token");
        when(github.getAllPullRequests(anyString(), anyString())).thenReturn(List.of());
        return new Fixture(owner, party, installation, first, second);
    }

    @Test
    @Transactional
    void repeatedDisconnectReturnsLatestHistory() {
        Fixture f = fixture();
        bindings.connect(f.party.getId(), f.first.getId(), f.owner);
        bindings.disconnect(f.party.getId(), f.owner);
        bindings.connect(f.party.getId(), f.second.getId(), f.owner);
        bindings.disconnect(f.party.getId(), f.owner);
        var status = connections.getStatus(f.party.getId(), f.owner);
        assertThat(status.status()).isEqualTo("DISCONNECTED");
        assertThat(status.repositoryFullName()).isEqualTo("org/second");
    }

    @Test
    @Transactional
    void removedRepositoryRequiresInstallationAndCanRecover() {
        Fixture f = fixture();
        bindings.connect(f.party.getId(), f.first.getId(), f.owner);
        inventory.markRepositoryRemoved(new GithubInstallationRepositoryRemovedEvent(
                f.installation.getInstallationId(), f.first.getRepositoryId()));
        var status = connections.getStatus(f.party.getId(), f.owner);
        assertThat(status.status()).isEqualTo("INSTALLATION_REQUIRED");
        assertThat(status.lastErrorCode()).isEqualTo("GITHUB_APP_REPOSITORY_REMOVED");
        f.first.refresh("org/first");
        assertThat(connections.getStatus(f.party.getId(), f.owner).status()).isEqualTo("ACTIVE");
    }

    @Test
    @Transactional
    void suspendedAndDeletedInstallationAreNotActive() {
        Fixture f = fixture();
        bindings.connect(f.party.getId(), f.first.getId(), f.owner);
        for (boolean deleted : List.of(false, true)) {
            inventory.markUnavailable(new GithubInstallationUnavailableEvent(f.installation.getInstallationId(), deleted));
            var status = connections.getStatus(f.party.getId(), f.owner);
            assertThat(status.status()).isEqualTo("INSTALLATION_REQUIRED");
            assertThat(status.lastErrorCode()).isEqualTo("GITHUB_APP_INSTALLATION_UNAVAILABLE");
        }
        bindings.disconnect(f.party.getId(), f.owner);
        assertThat(connections.getStatus(f.party.getId(), f.owner).status()).isEqualTo("DISCONNECTED");
    }

    @Test
    void concurrentConnectionsToDifferentRepositoriesAllowOnlyOneBinding() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Fixture f = tx.execute(ignored -> fixture());
        CountDownLatch firstSync = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        when(github.createInstallationToken(anyLong())).thenAnswer(ignored -> {
            firstSync.countDown();
            if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("sync timeout");
            return "installation-token";
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> bindings.connect(f.party.getId(), f.first.getId(), f.owner));
            assertThat(firstSync.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                return bindings.connect(f.party.getId(), f.second.getId(), f.owner);
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ServiceException.class)
                    .hasRootCauseMessage("409-20 : PARTY_GITHUB_REPOSITORY_ALREADY_CONNECTED");
            tx.executeWithoutResult(ignored -> assertThat(bindingRepository.findAll().stream()
                    .filter(binding -> binding.getParty().getId().equals(f.party.getId()))).hasSize(1));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
            tx.executeWithoutResult(ignored -> {
                audits.deleteAll(audits.findAll().stream()
                        .filter(audit -> audit.getParty().getId().equals(f.party.getId())).toList());
                bindingRepository.deleteAll(bindingRepository.findAll().stream()
                        .filter(binding -> binding.getParty().getId().equals(f.party.getId())).toList());
                repositories.deleteAll(List.of(f.first, f.second));
                installations.deleteById(f.installation.getId());
                parties.deleteById(f.party.getId());
            });
        }
    }

    private record Fixture(Member owner, Party party, GithubAppInstallation installation,
                           GithubInstallationRepository first, GithubInstallationRepository second) {}
}
