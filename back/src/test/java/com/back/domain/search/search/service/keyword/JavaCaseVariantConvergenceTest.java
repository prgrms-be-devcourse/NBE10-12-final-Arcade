package com.back.domain.search.search.service.keyword;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JavaCaseVariantConvergenceTest {

    @Autowired
    private KeywordExtractionPort keywordExtractionPort;

    @Autowired
    private KeywordNormalizationPort keywordNormalizationPort;

    private List<String> normalize(String query) {
        return keywordNormalizationPort.normalize(keywordExtractionPort.extract(query));
    }

    @Test
    void javaCaseVariantsAllConvergeToTheSameCanonicalTerm() {
        assertThat(normalize("java")).contains("자바");
        assertThat(normalize("JAVA")).contains("자바");
        assertThat(normalize("Java")).contains("자바");
        assertThat(normalize("자바")).contains("자바");
    }
}
