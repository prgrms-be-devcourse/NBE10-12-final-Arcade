package com.back.domain.search.search.service;

import java.util.List;

public interface KeywordExtractor {
    List<String> extract(String text);
}
