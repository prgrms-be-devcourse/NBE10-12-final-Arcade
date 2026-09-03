package com.back.domain.search.search.service;

import java.util.List;

public interface KeywordNormalizer {
    List<String> normalize(List<String> keywords);
}
