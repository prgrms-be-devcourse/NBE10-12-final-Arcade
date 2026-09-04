package com.back.domain.search.search.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.search.search.entity.SearchLog;
import com.back.domain.search.search.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchLogService {

    private final SearchLogRepository searchLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Member actor, String keyword) {
        if (actor == null) {
            return;
        }

        searchLogRepository.save(new SearchLog(actor, keyword));
    }
}
