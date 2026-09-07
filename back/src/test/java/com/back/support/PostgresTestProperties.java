package com.back.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

public final class PostgresTestProperties {

    private PostgresTestProperties() {
    }

    public static void register(DynamicPropertyRegistry registry, PostgreSQLContainer container) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.datasource.driver-class-name", container::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    public static void registerForProdProfile(DynamicPropertyRegistry registry, PostgreSQLContainer container) {
        register(registry, container);
        registry.add("custom.notification.redis-pubsub.enabled", () -> "false");
    }
}
