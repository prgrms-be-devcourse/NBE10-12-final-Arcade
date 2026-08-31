package com.back.domain.contest.contest.service;

import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.Contest;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestPost;
import com.back.domain.contest.contest.entity.ContestSortOption;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.repository.ContestPostRepository;
import com.back.domain.contest.contest.repository.ContestRepository;
import com.back.domain.member.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestPostRepository contestPostRepository;

    public long count() {
        return contestRepository.count();
    }

    @Transactional
    public ContestResponseDto write(Member actor, String title, ContestFormat format, ContestTag contestTag, LocalDate applicationPeriodStart, LocalDate applicationPeriodEnd, String description, String linkUrl, String imageUrl)
    {
        Contest contest = new Contest(actor.getId(), title, format, contestTag, applicationPeriodStart, applicationPeriodEnd);
        contestRepository.save(contest);
        ContestPost contestPost = new ContestPost(contest, description, linkUrl, imageUrl);
        contestPostRepository.save(contestPost);
        return new ContestResponseDto(contest, contestPost);
    }

    public Page<ContestResponseDto> list(ContestFormat format, ContestTag contestTag, ContestSortOption sortOption, Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        ContestSortOption effectiveSortOption = sortOption != null ? sortOption : ContestSortOption.LATEST;

        Page<ContestPost> posts = switch (effectiveSortOption) {
            case LATEST -> contestPostRepository.searchOrderByLatest(format, contestTag, unsorted);
            case POPULAR -> contestPostRepository.searchOrderByPopular(format, contestTag, unsorted);
            case DEADLINE -> contestPostRepository.searchOrderByDeadline(format, contestTag, unsorted);
        };

        return posts.map(contestPost -> new ContestResponseDto(contestPost.getContest(), contestPost));
    }

    @Transactional
    public Optional<ContestResponseDto> getDetail(long contestId, boolean countView) {
        return contestRepository.findById(contestId)
                .map(contest -> {
                    ContestPost contestPost = contestPostRepository.findByContest(contest).orElse(null);

                    if (contestPost != null && countView) {
                        contestPostRepository.increaseViewCount(contestId);
                        contestPost = contestPostRepository.findByContest(contest).orElseThrow();
                    }

                    return new ContestResponseDto(contest, contestPost);
                });
    }

    @Transactional
    public ContestResponseDto modify(long contestId, String title, String description, LocalDate start, LocalDate end, String linkUrl, String imageUrl) {
        Contest contest = contestRepository.findById(contestId).orElseThrow();
        ContestPost contestPost = contestPostRepository.findByContest(contest).orElseThrow();
        contest.modify(title,start, end);
        contestPost.modify(description, linkUrl, imageUrl);
        return new ContestResponseDto(contest, contestPost);
    }
    @Transactional
    public void deletePost(long contestId) {
        Contest contest = contestRepository.findById(contestId).orElseThrow();
        contestPostRepository.findByContest(contest).ifPresent(contestPostRepository::delete);
    }

    //00시 기준으로 모집 기한이 지난 대회글(ContestPost) 스케줄러 조회 후 삭제
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredPosts() {
        contestPostRepository.deleteAllByContest_ApplicationPeriodEndBefore(LocalDate.now());
    }
}
