package com.back.domain.search.search.service.keyword;

import java.util.List;

public interface KeywordNormalizationPort {
    List<String> normalize(List<String> keywords);
}
