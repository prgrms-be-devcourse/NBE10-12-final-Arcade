package com.back.domain.search.search.service.party;

import com.back.global.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PartyMatchQueryLikeServiceInvalidFilterTest {

    @Autowired
    private PartyMatchQueryLikeService partyMatchQueryLikeService;

    @Test
    void invalidPartyTagThrowsServiceExceptionInsteadOfIllegalArgumentException() {
        assertThatThrownBy(() ->
                partyMatchQueryLikeService.findMatchingPartyIds(
                        List.of("백엔드"), "NOT_A_REAL_TAG", null, null, PageRequest.of(0, 10)
                )
        )
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getRsData().resultCode()).isEqualTo("400-1"));
    }

    @Test
    void invalidTopicTypeThrowsServiceExceptionInsteadOfIllegalArgumentException() {
        assertThatThrownBy(() ->
                partyMatchQueryLikeService.findMatchingPartyIds(
                        List.of("백엔드"), null, "NOT_A_REAL_TOPIC", null, PageRequest.of(0, 10)
                )
        )
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getRsData().resultCode()).isEqualTo("400-1"));
    }

    @Test
    void invalidPositionTypeThrowsServiceExceptionInsteadOfIllegalArgumentException() {
        assertThatThrownBy(() ->
                partyMatchQueryLikeService.findMatchingPartyIds(
                        List.of("백엔드"), null, null, "NOT_A_REAL_POSITION", PageRequest.of(0, 10)
                )
        )
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getRsData().resultCode()).isEqualTo("400-1"));
    }
}
