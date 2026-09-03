package com.back.domain.search.search.service;

import java.util.List;

public interface KeywordExtractionPort {
    List<String> extract(String text);
}
