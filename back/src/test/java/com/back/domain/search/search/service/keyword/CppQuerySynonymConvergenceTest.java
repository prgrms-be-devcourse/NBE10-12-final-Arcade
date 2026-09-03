package com.back.domain.search.search.service.keyword;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CppQuerySynonymConvergenceTest {

    @Autowired
    private KeywordExtractionPort keywordExtractionPort;

    @Autowired
    private KeywordNormalizationPort keywordNormalizationPort;

    @Test
    void queryingCppSymbolFormConvergesWithPlainCppSpelling() {
        List<String> fromSymbolQuery = keywordNormalizationPort.normalize(keywordExtractionPort.extract("C++"));
        List<String> fromPlainTitle = keywordNormalizationPort.normalize(keywordExtractionPort.extract("cpp 스터디"));

        assertThat(fromSymbolQuery).contains("cpp");
        assertThat(fromPlainTitle).contains("cpp");
    }
}
