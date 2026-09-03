package com.back.domain.search.search.service.keyword;

import com.back.domain.search.search.entity.keyword.KeywordSynonym;
import com.back.domain.search.search.repository.keyword.KeywordSynonymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KeywordSynonymSeeder implements CommandLineRunner {

    private final KeywordSynonymRepository keywordSynonymRepository;

    private static final Map<String, String> SEED_SYNONYMS = Map.ofEntries(
            Map.entry("backend", "백엔드"),
            Map.entry("프론트", "프론트엔드"),
            Map.entry("frontend", "프론트엔드"),
            Map.entry("developer", "개발자"),

            Map.entry("스터디원", "스터디"),
            Map.entry("토이프로젝트", "프로젝트"),
            Map.entry("사이드프로젝트", "프로젝트"),

            Map.entry("웹", "웹개발"),
            Map.entry("앱", "앱개발"),
            Map.entry("모바일", "앱개발"),
            Map.entry("게임", "게임개발"),

            Map.entry("javascript", "자바스크립트"),
            Map.entry("js", "자바스크립트"),
            Map.entry("typescript", "타입스크립트"),
            Map.entry("ts", "타입스크립트"),
            Map.entry("python", "파이썬"),
            Map.entry("kotlin", "코틀린"),
            Map.entry("java", "자바"),
            Map.entry("cpp", "C++"),
            Map.entry("csharp", "C#"),
            Map.entry("swift", "스위프트"),
            Map.entry("golang", "Go"),
            Map.entry("rust", "러스트"),
            Map.entry("dart", "다트"),

            Map.entry("react", "리액트"),
            Map.entry("vue", "뷰"),
            Map.entry("vuejs", "뷰"),
            Map.entry("next", "넥스트"),
            Map.entry("nextjs", "넥스트"),
            Map.entry("angular", "앵귤러"),
            Map.entry("svelte", "스벨트"),
            Map.entry("jquery", "제이쿼리"),
            Map.entry("tailwind", "테일윈드"),
            Map.entry("tailwindcss", "테일윈드"),

            Map.entry("spring", "스프링"),
            Map.entry("springboot", "스프링부트"),
            Map.entry("express", "익스프레스"),
            Map.entry("expressjs", "익스프레스"),
            Map.entry("django", "장고"),
            Map.entry("flask", "플라스크"),
            Map.entry("nestjs", "네스트"),
            Map.entry("node", "노드"),
            Map.entry("nodejs", "노드"),
            Map.entry("fastapi", "패스트API"),
            Map.entry("laravel", "라라벨"),
            Map.entry("rails", "레일즈"),

            Map.entry("postgres", "포스트그레스"),
            Map.entry("postgresql", "포스트그레스"),
            Map.entry("mongodb", "몽고디비"),
            Map.entry("mongo", "몽고디비"),
            Map.entry("oracle", "오라클"),
            Map.entry("firebase", "파이어베이스"),
            Map.entry("dynamodb", "다이나모디비"),
            Map.entry("elasticsearch", "엘라스틱서치"),

            Map.entry("flutter", "플러터"),
            Map.entry("reactnative", "리액트네이티브"),
            Map.entry("android", "안드로이드"),

            Map.entry("kubernetes", "쿠버네티스"),
            Map.entry("k8s", "쿠버네티스"),
            Map.entry("docker", "도커"),
            Map.entry("jenkins", "젠킨스"),
            Map.entry("terraform", "테라폼"),
            Map.entry("nginx", "엔진엑스"),
            Map.entry("azure", "애저"),

            Map.entry("github", "깃허브"),
            Map.entry("gitlab", "깃랩"),

            Map.entry("인공지능", "AI"),
            Map.entry("machinelearning", "머신러닝"),
            Map.entry("deeplearning", "딥러닝"),
            Map.entry("dl", "딥러닝"),
            Map.entry("tensorflow", "텐서플로우"),
            Map.entry("pytorch", "파이토치"),

            Map.entry("figma", "피그마"),
            Map.entry("notion", "노션"),
            Map.entry("postman", "포스트맨"),
            Map.entry("jest", "제스트")
    );

    @Override
    public void run(String... args) {
        if (keywordSynonymRepository.count() > 0) {
            return;
        }

        List<KeywordSynonym> synonyms = SEED_SYNONYMS.entrySet().stream()
                .map(entry -> new KeywordSynonym(entry.getKey(), entry.getValue()))
                .toList();

        keywordSynonymRepository.saveAll(synonyms);
    }
}
