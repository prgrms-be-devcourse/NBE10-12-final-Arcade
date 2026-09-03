package com.back.domain.search.search.service;

import java.util.List;

public interface RelatedTermExpansionPort {
    List<String> expand(List<String> keywords);
}
