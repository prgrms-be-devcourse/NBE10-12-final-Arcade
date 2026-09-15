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
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class PartyGithubBindingRegressionTest {
    @Autowired PartyGithubBindingService bindings;
    @Autowired PartyGithubConnectionService connections;
    @Autowired GithubInstallationInventoryService inventory;
    @Autowired PartyRepository parties;
    @Autowired MemberRepository members;
    @Autowired GithubAppInstallationRepository installations;
    @MockitoSpyBean GithubInstallationRepositoryRepository repositories;
    @Autowired PartyGithubBindingRepository bindingRepository;
    @Autowired PartyGithubBindingAuditRepository audits;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @MockitoBean GithubAppUserAuthorizationService authorization;
    @MockitoBean GithubAppUserRepositoryAccessService access;
    @MockitoBean GithubAppClient github;
    @MockitoBean com.back.global.github.service.GithubWebhookVerifier webhookVerifier;
    @Autowired com.back.global.github.service.GithubWebhookService webhooks;
    @Autowired com.back.global.github.repository.GithubWebhookDeliveryRepository deliveries;
    private static final AtomicLong IDS = new AtomicLong(900000);

    private Fixture current;
    private Fixture fixture() {
        current = new TransactionTemplate(transactionManager).execute(ignored -> createFixture());
        return current;
    }

    @AfterEach
    void cleanup() {
        if (current == null) return;
        Fixture f = current;
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            audits.deleteAll(audits.findAll().stream().filter(a -> a.getParty().getId().equals(f.party.getId())).toList());
            bindingRepository.deleteAll(bindingRepository.findAll().stream()
                    .filter(b -> b.getParty().getId().equals(f.party.getId())).toList());
            repositories.deleteAll(repositories.findAllByInstallationInstallationId(f.installation.getInstallationId()));
            installations.deleteById(f.installation.getId());
            parties.deleteById(f.party.getId());
        });
    }

    private Fixture createFixture() {
        Member owner = members.findByEmail("user1@test.com").orElseThrow();
        Party party = parties.save(new Party(owner, "연결 테스트", "테스트", "설명", null, null, null,
                TopicType.PROJECT, PartyTag.WEB, null, LocalDateTime.now().plusDays(7)));
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
    void removedRepositoryRequiresInstallationAndCanRecover() {
        Fixture f = fixture();
        bindings.connect(f.party.getId(), f.first.getId(), f.owner);
        inventory.markRepositoryRemoved(new GithubInstallationRepositoryRemovedEvent(
                f.installation.getInstallationId(), f.first.getRepositoryId()));
        var status = connections.getStatus(f.party.getId(), f.owner);
        assertThat(status.status()).isEqualTo("INSTALLATION_REQUIRED");
        assertThat(status.lastErrorCode()).isEqualTo("GITHUB_APP_REPOSITORY_REMOVED");
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored ->
                repositories.findById(f.first.getId()).orElseThrow().refresh("org/first"));
        assertThat(connections.getStatus(f.party.getId(), f.owner).status()).isEqualTo("ACTIVE");
    }

    @Test
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
        Fixture f = fixture();
        CountDownLatch firstSync = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        when(github.createInstallationToken(anyLong())).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            if (calls.incrementAndGet() == 1) {
                firstSync.countDown();
                if (!releaseFirst.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("sync timeout");
            }
            return "installation-token";
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> bindings.connect(f.party.getId(), f.first.getId(), f.owner));
            assertThat(firstSync.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(() -> bindings.connect(f.party.getId(), f.second.getId(), f.owner));
            // 첫 요청의 외부 API가 지연돼도 두 번째 요청은 DB 잠금을 기다리지 않는다.
            second.get(5, TimeUnit.SECONDS);
            releaseFirst.countDown();
            assertThatThrownBy(() -> first.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ServiceException.class)
                    .hasRootCauseMessage("409-20 : PARTY_GITHUB_REPOSITORY_ALREADY_CONNECTED");
            tx.executeWithoutResult(ignored -> assertThat(bindingRepository.findAll().stream()
                    .filter(binding -> binding.getParty().getId().equals(f.party.getId()))).hasSize(1));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        }
    }

    @Test
    void githubFailureDoesNotLeaveBinding() {
        Fixture f = fixture();
        when(github.getAllPullRequests(anyString(), anyString())).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            throw new IllegalStateException("GitHub unavailable");
        });
        assertThatThrownBy(() -> bindings.connect(f.party.getId(), f.first.getId(), f.owner))
                .hasMessage("GitHub unavailable");
        assertThat(connections.getStatus(f.party.getId(), f.owner).status()).isEqualTo("NOT_CONNECTED");
    }

    @Test
    void repositoryRemovedDuringGithubCallCannotBeConnected() {
        Fixture f = fixture();
        when(github.getAllPullRequests(anyString(), anyString())).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            inventory.markRepositoryRemoved(new GithubInstallationRepositoryRemovedEvent(
                    f.installation.getInstallationId(), f.first.getRepositoryId()));
            return List.of();
        });
        assertThatThrownBy(() -> bindings.connect(f.party.getId(), f.first.getId(), f.owner))
                .hasMessage("409-20 : GITHUB_INSTALLATION_REPOSITORY_UNAVAILABLE");
    }

    @Test
    void repositoryQueryRestrictsInstallationIdsRepositoryIdsAndStatus() {
        Fixture f = fixture();
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            assertThat(repositories.findAccessibleRepositories(f.installation.getInstallationId(),
                    List.of(f.first.getRepositoryId()), GithubInstallationRepositoryStatus.AVAILABLE))
                    .extracting(GithubInstallationRepository::getId).containsExactly(f.first.getId());
            assertThat(repositories.findAccessibleRepositories(-1, List.of(f.first.getRepositoryId()),
                    GithubInstallationRepositoryStatus.AVAILABLE)).isEmpty();
            repositories.findById(f.first.getId()).orElseThrow().remove();
            assertThat(repositories.findAccessibleRepositories(f.installation.getInstallationId(),
                    List.of(f.first.getRepositoryId()), GithubInstallationRepositoryStatus.AVAILABLE)).isEmpty();
        });
    }

    @Test
    void inventoryFetchesOutsideTransactionAndReadsExistingRepositoriesOnce() {
        Fixture f = fixture();
        long addedId = IDS.incrementAndGet();
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored ->
                repositories.findById(f.first.getId()).orElseThrow().remove());
        when(github.getInstallationSnapshot(anyLong())).thenCallRealMethod();
        when(github.getInstallation(anyLong())).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new GithubAppClient.Installation(f.installation.getInstallationId(), 123L, "renamed-org", "Organization");
        });
        when(github.createInstallationToken(anyLong())).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "token";
        });
        when(github.getAllInstallationRepositories(anyString())).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return List.of(new GithubAppClient.Repository(f.first.getRepositoryId(), "org/renamed"),
                    new GithubAppClient.Repository(addedId, "org/added"));
        });
        clearInvocations(repositories);
        inventory.syncInstallation(f.installation.getInstallationId());
        verify(repositories, times(1)).findAllByInstallationInstallationId(f.installation.getInstallationId());
        verify(repositories, never()).findByInstallationInstallationIdAndRepositoryId(any(), any());
        var all = repositories.findAllByInstallationInstallationId(f.installation.getInstallationId());
        assertThat(all).hasSize(3);
        assertThat(all).filteredOn(r -> r.getRepositoryId().equals(f.first.getRepositoryId()))
                .allSatisfy(r -> {
                    assertThat(r.getFullName()).isEqualTo("org/renamed");
                    assertThat(r.getStatus()).isEqualTo(GithubInstallationRepositoryStatus.AVAILABLE);
                });
        assertThat(all).filteredOn(r -> r.getRepositoryId().equals(f.second.getRepositoryId()))
                .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(GithubInstallationRepositoryStatus.REMOVED));
    }

    @Test
    void inventoryApiFailureDoesNotPartiallyUpdateDatabase() {
        Fixture f = fixture();
        when(github.getInstallationSnapshot(anyLong())).thenCallRealMethod();
        when(github.getInstallation(anyLong())).thenReturn(new GithubAppClient.Installation(
                f.installation.getInstallationId(), 123L, "should-not-be-saved", "Organization"));
        when(github.getAllInstallationRepositories(anyString())).thenThrow(new IllegalStateException("GitHub unavailable"));
        assertThatThrownBy(() -> inventory.syncInstallation(f.installation.getInstallationId()))
                .hasMessage("GitHub unavailable");
        assertThat(installations.findByInstallationId(f.installation.getInstallationId()).orElseThrow().getAccountLogin()).isEqualTo("org");
        assertThat(repositories.findAllByInstallationInstallationId(f.installation.getInstallationId()))
                .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(GithubInstallationRepositoryStatus.AVAILABLE));
    }

    @Test
    void emptyInventoryRemovesEveryExistingRepository() {
        Fixture f = fixture();
        when(github.getInstallationSnapshot(anyLong())).thenReturn(new com.back.global.github.client.dtos.GithubInstallationSnapshot(
                new GithubAppClient.Installation(f.installation.getInstallationId(), 123L, "org", "Organization"), List.of()));
        inventory.syncInstallation(f.installation.getInstallationId());
        assertThat(repositories.findAllByInstallationInstallationId(f.installation.getInstallationId()))
                .hasSize(2).allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(GithubInstallationRepositoryStatus.REMOVED));
    }

    @Test
    void webhookFetchFailureCanBeRetriedAndDuplicateDeliveryIsSkipped() {
        Fixture f = fixture();
        String deliveryId = java.util.UUID.randomUUID().toString();
        byte[] body = ("{\"action\":\"created\",\"installation\":{\"id\":"
                + f.installation.getInstallationId() + "}}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(github.getInstallationSnapshot(anyLong())).thenAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            throw new IllegalStateException("GitHub unavailable");
        });
        assertThatThrownBy(() -> webhooks.receive("installation", "signature", deliveryId, body))
                .hasMessage("GitHub unavailable");
        assertThat(deliveries.existsByDeliveryId(deliveryId)).isFalse();
        doAnswer(ignored -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new com.back.global.github.client.dtos.GithubInstallationSnapshot(
                    new GithubAppClient.Installation(f.installation.getInstallationId(), 123L, "org", "Organization"), List.of());
        }).when(github).getInstallationSnapshot(anyLong());
        try {
            webhooks.receive("installation", "signature", deliveryId, body);
            assertThat(deliveries.existsByDeliveryId(deliveryId)).isTrue();
            assertThat(repositories.findAllByInstallationInstallationId(f.installation.getInstallationId()))
                    .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(GithubInstallationRepositoryStatus.REMOVED));
            webhooks.receive("installation", "signature", deliveryId, body);
            verify(github, times(2)).getInstallationSnapshot(f.installation.getInstallationId());
        } finally {
            new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> entityManager
                    .createQuery("delete from GithubWebhookDelivery d where d.deliveryId = :deliveryId")
                    .setParameter("deliveryId", deliveryId).executeUpdate());
        }
    }

    private record Fixture(Member owner, Party party, GithubAppInstallation installation,
                           GithubInstallationRepository first, GithubInstallationRepository second) {}
}
