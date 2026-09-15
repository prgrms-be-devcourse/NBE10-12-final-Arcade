package com.back.domain.search.search.service.keyword;

import com.back.domain.search.search.entity.keyword.KeywordRelatedTerm;
import com.back.domain.search.search.repository.keyword.KeywordRelatedTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class KeywordRelatedTermSeeder implements CommandLineRunner {

    private final KeywordRelatedTermRepository keywordRelatedTermRepository;

    private record BridgePair(String a, String b) { }

    private static final List<List<String>> RELATED_GROUPS = List.of(
            List.of("서버", "백엔드", "devops", "인프라", "sre", "시스템엔지니어"),
            List.of("기획", "pm", "po", "프로덕트매니저", "프로덕트오너", "서비스기획자", "디렉터"),
            List.of("클라이언트", "프론트엔드", "게임클라이언트"),
            List.of("디자인", "uiux", "ui", "ux", "프로덕트디자인", "그래픽디자인"),
            List.of("qa", "테스터", "sqa", "품질관리"),
            List.of("데이터분석가", "데이터엔지니어", "데이터사이언티스트", "데이터분석", "ai", "머신러닝"),
            List.of("마케팅", "마케터", "그로스마케팅", "퍼포먼스마케팅", "그로스해킹")
    );

    private static final List<BridgePair> BRIDGE_PAIRS = List.of(
            new BridgePair("풀스택", "백엔드"),
            new BridgePair("풀스택", "서버"),
            new BridgePair("풀스택", "프론트엔드"),
            new BridgePair("풀스택", "클라이언트")
    );

    @Override
    public void run(String... args) {
        if (keywordRelatedTermRepository.count() > 0) {
            return;
        }

        List<KeywordRelatedTerm> relations = new ArrayList<>();
        RELATED_GROUPS.forEach(group -> toPairwiseRelations(group).forEach(relations::add));
        BRIDGE_PAIRS.forEach(pair -> {
            relations.add(new KeywordRelatedTerm(pair.a(), pair.b()));
            relations.add(new KeywordRelatedTerm(pair.b(), pair.a()));
        });

        keywordRelatedTermRepository.saveAll(relations);
    }

    private Stream<KeywordRelatedTerm> toPairwiseRelations(List<String> group) {
        return group.stream()
                .flatMap(term -> group.stream()
                        .filter(other -> !other.equals(term))
                        .map(other -> new KeywordRelatedTerm(term, other)));
    }
}
