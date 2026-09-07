package com.back.domain.party.party.repository;

import com.back.support.PostgresTestProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class PartyRepositoryEmptyInTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        PostgresTestProperties.register(registry, POSTGRES);
    }

    @Autowired
    private PartyRepository partyRepository;

    @Test
    void findAllByIdInWithEmptyListDoesNotThrow() {
        List<?> result = partyRepository.findAllByIdIn(List.of());

        assertThat(result).isEmpty();
    }
}
