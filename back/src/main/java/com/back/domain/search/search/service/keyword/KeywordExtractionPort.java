package com.back.domain.search.search.service.keyword;

import java.util.List;

public interface KeywordExtractionPort {
    List<String> extract(String text);
}
