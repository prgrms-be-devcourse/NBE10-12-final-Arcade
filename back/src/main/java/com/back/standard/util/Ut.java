package com.back.standard.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.SneakyThrows;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class Ut {
    public static class jwt {
        public static String toString(String secret, int expireSeconds, Map<String, Object> body) {
            ClaimsBuilder claimsBuilder = Jwts.claims();

            for (Map.Entry<String, Object> entry : body.entrySet()) {
                claimsBuilder.add(entry.getKey(), entry.getValue());
            }

            Claims claims = claimsBuilder.build();

            Date issuedAt = new Date();
            Date expiration = new Date(issuedAt.getTime() + 1000L * expireSeconds);

            Key secretKey = Keys.hmacShaKeyFor(secret.getBytes());

            String jwt = Jwts.builder()
                    .claims(claims)
                    .issuedAt(issuedAt)
                    .expiration(expiration)
                    .signWith(secretKey)
                    .compact();

            return jwt;
        }

        public static boolean isValid(String secret, String jwtStr) {
            SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

            try {
                Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(jwtStr);
            } catch (Exception e) {
                return false;
            }

            return true;
        }

        public static Map<String, Object> payload(String secret, String jwtStr) {
            SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());

            try {
                return new LinkedHashMap<>(
                        Jwts.parser()
                                .verifyWith(secretKey)
                                .build()
                                .parseSignedClaims(jwtStr)
                                .getPayload()
                );
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static class json {
        public static ObjectMapper objectMapper;

        public static String toString(Object object) {
            return toString(object, null);
        }

        public static String toString(Object object, String defaultValue) {
            try {
                return objectMapper.writeValueAsString(object);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    public static class cmd {
        @SneakyThrows
        public static void run(String... args) {
            boolean isWindows = System
                    .getProperty("os.name")
                    .toLowerCase()
                    .contains("win");

            ProcessBuilder builder = new ProcessBuilder(
                    Arrays.stream(args)
                            .map(arg -> arg.replace("{{DOT_CMD}}", isWindows ? ".cmd" : ""))
                            .toArray(String[]::new)
            );

            // 에러 스트림도 출력 스트림과 함께 병합
            builder.redirectErrorStream(true);

            // 프로세스 시작
            Process process = builder.start();

            // 결과 출력
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line); // 결과 한 줄씩 출력
                }
            }

            // 종료 코드 확인
            int exitCode = process.waitFor();
            System.out.println("종료 코드: " + exitCode);
        }

        public static void runAsync(String... args) {
            new Thread(() -> {
                run(args);
            }).start();
        }
    }
}