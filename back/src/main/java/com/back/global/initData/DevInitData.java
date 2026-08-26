package com.back.global.initData;

import com.back.standard.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class DevInitData {
    @Bean
    ApplicationRunner devInitDataApplicationRunner() {
        return args -> {
            Ut.cmd.runAsync(
                    "npx{{DOT_CMD}}",
                    "--yes",
                    "--package", "typescript@v5",
                    "--package", "openapi-typescript",
                    "openapi-typescript", "http://localhost:8080/v3/api-docs/apiV1",
                    "-o", "../front/src/global/backend/apiV1/schema.d.ts",
                    "--properties-required-by-default"
            );
        };
    }
}