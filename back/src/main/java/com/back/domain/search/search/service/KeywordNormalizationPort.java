package com.back.domain.search.search.service;

import java.util.List;

public interface KeywordNormalizationPort {
    List<String> normalize(List<String> keywords);
}
