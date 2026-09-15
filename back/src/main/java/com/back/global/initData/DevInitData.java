package com.back.global.initData;

import com.back.domain.activity.activity.entity.ActivityLog;
import com.back.domain.activity.activity.repository.ActivityLogRepository;
import com.back.domain.activity.activity.service.ActivityLogService;
import com.back.domain.contest.contest.dtos.ContestResponseDto;
import com.back.domain.contest.contest.entity.ContestFormat;
import com.back.domain.contest.contest.entity.ContestTag;
import com.back.domain.contest.contest.service.ContestService;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.service.GoalService;
import com.back.domain.goal.goal.dtos.GoalCreateReqBody;
import com.back.domain.goal.goal.dtos.GoalDetailReqBody;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.service.MemberService;
import com.back.domain.message.message.service.MessageService;
import com.back.domain.notification.notification.entity.Notification;
import com.back.domain.notification.notification.entity.NotificationType;
import com.back.domain.notification.notification.service.NotificationService;
import com.back.domain.party.application.service.PartyApplicationService;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import com.back.domain.party.showcase.service.PartyShowcaseService;
import com.back.domain.party.party.entity.PartyTag;
import com.back.domain.party.party.entity.TopicType;
import com.back.domain.party.party.dtos.PartyDto;
import com.back.domain.party.party.service.PartyLifecycleService;
import com.back.domain.party.party.service.PartyService;
import com.back.standard.util.Util;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class DevInitData {
    @Autowired
    @Lazy
    private DevInitData self;
    private final MemberService memberService;
    private final ContestService contestService;
    private final PartyService partyService;
    private final PartyLifecycleService partyLifecycleService;
    private final PartyApplicationService partyApplicationService;
    private final GoalService goalService;
    private final NotificationService notificationService;
    private final MessageService messageService;
    private final ActivityLogRepository activityLogRepository;
    private final PartyShowcaseService partyShowcaseService;
    private final PartyShowcaseRepository partyShowcaseRepository;

    @Value("${custom.openapi.api-docs-url}")
    private String apiDocsUrl;

    /** 가입한 본인 계정에도 샘플을 채우고 싶을 때 .env 에 DEV_SEED_EMAIL 로 지정한다 */
    @Value("${DEV_SEED_EMAIL:}")
    private String devSeedEmail;

    @Bean
    @Order(2)
    ApplicationRunner devInitDataApplicationRunner() {
        return args -> {
            self.createSampleData();
            self.createPersonalSampleData();
            self.createActivitySampleData();
            self.createShowcaseSampleData();
            Util.cmd.runAsync(
                    "npx{{DOT_CMD}}",
                    "--yes",
                    "--package", "typescript@v5",
                    "--package", "openapi-typescript",
                    "openapi-typescript", apiDocsUrl,
                    "-o", "../front/src/global/backend/apiV1/schema.d.ts",
                    "--properties-required-by-default"
            );
        };
    }

    @Transactional
    public void createSampleData() {
        if (contestService.count() > 0) return;

        Member user1 = memberService.findByEmail("user1@test.com").orElseThrow();
        Member user2 = memberService.findByEmail("user2@test.com").orElseThrow();
        Member user3 = memberService.findByEmail("user3@test.com").orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        ContestResponseDto aiHackathon = contestService.write(user1, "2026 AI 서비스 해커톤", ContestFormat.HACKATHON, ContestTag.AI, today.minusDays(4), today.plusDays(18), "생성형 AI를 활용해 일상의 문제를 해결하는 48시간 팀 해커톤입니다.", "https://example.com/contests/ai-hackathon", "https://placehold.co/1200x630?text=AI+Hackathon");
        ContestResponseDto dataContest = contestService.write(user2, "공공데이터 분석 경진대회", ContestFormat.CONTEST, ContestTag.DATA, today.minusDays(10), today.plusDays(7), "공공데이터로 지역 문제를 분석하고 실행 가능한 정책 아이디어를 제안합니다.", "https://example.com/contests/public-data", "https://placehold.co/1200x630?text=Public+Data");
        ContestResponseDto fintechContest = contestService.write(user3, "핀테크 UX 챌린지", ContestFormat.CONTEST, ContestTag.FINTECH, today.plusDays(2), today.plusDays(28), "더 쉬운 금융 경험을 만드는 서비스 기획과 프로토타입을 모집합니다.", "https://example.com/contests/fintech-ux", "https://placehold.co/1200x630?text=Fintech+UX");
        contestService.write(user1, "친환경 앱 아이디어톤", ContestFormat.HACKATHON, ContestTag.ENVIRONMENT, today.minusDays(20), today.minusDays(1), "지난 친환경 아이디어톤입니다. 종료·아카이브 화면 테스트에 사용하세요.", "https://example.com/contests/green", "https://placehold.co/1200x630?text=Green+Idea");

        createInProgressParty(user1, "user1 진행 파티", "user1의 GitHub App 설치 테스트 파티", "https://github.com/example/user1-in-progress", now.plusDays(30));
        createInProgressParty(user2, "user2 진행 파티", "user2의 GitHub App 설치 테스트 파티", "https://github.com/example/user2-in-progress", now.plusDays(30));
        createInProgressParty(user3, "user3 진행 파티", "user3의 GitHub App 설치 테스트 파티", "https://github.com/example/user3-in-progress", now.plusDays(30));

        PartyDto aiCrew = partyService.create(user1, "AI 크루", "AI 해커톤 MVP를 함께 만들 팀원을 찾습니다", "RAG 기반 학습 코치 서비스를 만들 예정입니다. 주 2회 온라인 미팅, 데모까지 함께해요.", aiHackathon.id(), null, null, TopicType.CONTEST, PartyTag.WEB, "https://github.com/example/ai-crew", now.plusDays(12), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 1), new PartyService.PositionCreateSpec(PositionType.FRONT, 2), new PartyService.PositionCreateSpec(PositionType.UIUX, 1)));
        PartyDto dataExplorers = partyService.create(user2, "데이터 탐험대", "공공데이터 분석 경진대회 팀원 모집", "분석 결과를 시민이 이해하기 쉬운 대시보드로 보여줄 팀입니다.", dataContest.id(), null, null, TopicType.CONTEST, PartyTag.WEB, "https://github.com/example/data-explorers", now.plusDays(5), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 1), new PartyService.PositionCreateSpec(PositionType.FRONT, 1), new PartyService.PositionCreateSpec(PositionType.PM, 1)));
        PartyDto fintechSprint = partyService.create(user3, "금융 UX 스프린트", "핀테크 UX 챌린지 프로토타입 팀", "Figma부터 모바일 프로토타입까지 빠르게 검증할 분을 찾습니다.", fintechContest.id(), null, null, TopicType.CONTEST, PartyTag.APP, null, now.plusDays(20), List.of(new PartyService.PositionCreateSpec(PositionType.UIUX, 2), new PartyService.PositionCreateSpec(PositionType.FRONT, 1), new PartyService.PositionCreateSpec(PositionType.PM, 1)));
        PartyDto springStudy = partyService.create(user1, "스프링 스터디", "실전 코드리뷰 중심 Spring Boot 스터디", "매주 한 주제씩 구현하고 PR 리뷰를 진행합니다.", null, null, null, TopicType.STUDY, PartyTag.WEB, "https://github.com/example/spring-study", now.plusDays(9), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 3), new PartyService.PositionCreateSpec(PositionType.PM, 1)));
        partyService.create(user2, "주말 인디게임", "2주 안에 완성하는 캐주얼 게임 프로젝트", "Unity 경험이 없어도 기획과 아트, 개발을 함께 배우며 진행합니다.", null, "2026 인디게임 공모전", "https://example.com/contests/indie-game", TopicType.CONTEST, PartyTag.GAME, null, now.plusDays(14), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 1), new PartyService.PositionCreateSpec(PositionType.UIUX, 2), new PartyService.PositionCreateSpec(PositionType.PM, 1)));

        // GitHub App 설치·팀 공간을 확인할 수 있도록, 각 개발 계정이 파티장인 진행 중 파티를 만든다.
        // 생성 후 정식 모집 마감 흐름을 태워야 status가 IN_PROGRESS로 전환된다.

        // 마이페이지 관리 탭(?tab=manage) 테스트용. 실제 지원 흐름을 태워 지원자 알림도 함께 생긴다.
        apply(aiCrew, PositionType.BACK, user2, "Spring Boot 2년 차입니다. RAG 파이프라인 API를 맡고 싶어요.");
        apply(aiCrew, PositionType.FRONT, user3, "Next.js로 채팅 UI 만들어 봤습니다.");
        apply(springStudy, PositionType.BACK, user3, "코드리뷰 문화를 경험해 보고 싶습니다.");
        apply(dataExplorers, PositionType.FRONT, user1, "대시보드 차트 작업 경험이 있어요.");
        apply(fintechSprint, PositionType.UIUX, user1, "Figma 프로토타이핑 가능합니다.");
        apply(fintechSprint, PositionType.PM, user2, "일정 관리와 발표 자료를 맡을게요.");

        createGoals(user1, user2, user3, today);
        createNotifications(user1, user2);
        createMessages(user1, user2, user3);
    }

    // 본인 계정은 서버를 띄운 뒤 가입하므로, 가입 후 재시작하면 채운다. 이미 받은 쪽지가 있으면 건너뛴다.
    @Transactional
    public void createPersonalSampleData() {
        if (devSeedEmail.isBlank()) return;
        Member me = memberService.findByEmail(devSeedEmail).orElse(null);
        if (me == null) return;
        if (messageService.getList(me, MessageService.MessageFilterOption.RECEIVED, 0, 1).totalElements() > 0) return;

        Member user1 = memberService.findByEmail("user1@test.com").orElseThrow();
        Member user2 = memberService.findByEmail("user2@test.com").orElseThrow();
        Member user3 = memberService.findByEmail("user3@test.com").orElseThrow();

        PartyDto myParty = partyService.create(me, "내 테스트 파티", "관리 탭 테스트용 파티", "지원자 관리 화면을 확인하기 위한 개발 데이터입니다.", null, null, null, TopicType.PROJECT, PartyTag.WEB, null, LocalDateTime.now().plusDays(14), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 2), new PartyService.PositionCreateSpec(PositionType.FRONT, 1), new PartyService.PositionCreateSpec(PositionType.UIUX, 1)));
        apply(myParty, PositionType.BACK, user1, "Spring Boot 2년 차입니다. 백엔드 맡고 싶어요.");
        apply(myParty, PositionType.FRONT, user2, "Next.js 경험 있습니다.");
        apply(myParty, PositionType.UIUX, user3, "Figma 프로토타이핑 가능합니다.");

        createMessages(me, user2, user3);
    }

    /**
     * 연속 활동(스트릭) 테스트용. 오늘 행은 파티 생성·지원이 record() 로 만들므로 어제 이전만 넣는다.
     * 계정마다 이틀 전 행이 이미 있으면 건너뛰어 재시작해도 겹치지 않는다.
     */
    @Transactional
    public void createActivitySampleData() {
        // 오늘 있음 + 어제부터 13일 연속 → 14일
        seedActivity(memberService.findByEmail("user1@test.com").orElse(null), 1, 13);
        // 어제가 비어 끊김 → 오늘만 세서 1일 (이틀 전~30일 전 기록은 히트맵에만 보임)
        seedActivity(memberService.findByEmail("user2@test.com").orElse(null), 2, 30);
        // 210일 연속이지만 조회 구간이 200일 → 201일 (상한 확인)
        seedActivity(memberService.findByEmail("user3@test.com").orElse(null), 1, 210);
        // 오늘 활동 없음 + 어제부터 5일 연속 → 5일 (오늘 비면 어제부터 센다)
        seedActivity(memberService.findByEmail("admin").orElse(null), 1, 5);
        // 본인 계정: 오늘(파티 생성) + 29일 연속 → 30일
        if (!devSeedEmail.isBlank()) seedActivity(memberService.findByEmail(devSeedEmail).orElse(null), 1, 29);
    }

    private void seedActivity(Member member, int fromDaysAgo, int toDaysAgo) {
        if (member == null) return;
        LocalDate today = LocalDate.now(ActivityLogService.ZONE);
        if (activityLogRepository.findByMemberAndActivityDate(member, today.minusDays(2)).isPresent()) return;

        for (int daysAgo = fromDaysAgo; daysAgo <= toDaysAgo; daysAgo++) {
            // 1~8 로 돌려 히트맵 농도 0~3 구간이 골고루 나오게 한다
            activityLogRepository.save(new ActivityLog(member, today.minusDays(daysAgo), daysAgo * 5 % 8 + 1));
        }
    }

    /**
     * 전시 기록 테스트용. 지원 → 승인 → 모집 마감 → 완료 → 전시 게시를 단계마다 따로 커밋한다.
     * 성취·GitHub 리스너가 AFTER_COMMIT 이라, 한 트랜잭션에 몰면 API 로 할 때와 결과가 달라진다.
     */
    public void createShowcaseSampleData() {
        Member user1 = memberService.findByEmail("user1@test.com").orElseThrow();
        Member user2 = memberService.findByEmail("user2@test.com").orElseThrow();
        Member user3 = memberService.findByEmail("user3@test.com").orElseThrow();

        if (partyShowcaseRepository.count() == 0) {
            completeParty(user1, "지난 해커톤 팀", List.of(user2, user3), "RAG 학습 코치", "48시간 해커톤에서 만든 RAG 기반 학습 코치 서비스입니다.");
            completeParty(user2, "공공데이터 대시보드 팀", List.of(user1), "우리 동네 대시보드", "공공데이터로 지역 문제를 시각화한 대시보드입니다.");
            // 완료했지만 전시를 올리지 않은 파티 - 마이페이지에서 '전시 페이지 보기' 링크가 없어야 한다
            completeParty(user3, "전시 미게시 완료 파티", List.of(user1), null, null);
        }

        if (devSeedEmail.isBlank()) return;
        Member me = memberService.findByEmail(devSeedEmail).orElse(null);
        if (me == null || partyShowcaseRepository.countPublishedByAssembledMember(me) > 0) return;

        completeParty(me, "내 완료 프로젝트", List.of(user1, user2), "내 포트폴리오 프로젝트", "파티장으로 완료하고 전시한 프로젝트입니다.");
        completeParty(user3, "참여한 완료 프로젝트", List.of(me), "팀원으로 참여한 프로젝트", "팀원으로 참여해 완료한 프로젝트입니다.");
        completeParty(user2, "참여했지만 미게시", List.of(me), null, null);
    }

    private void completeParty(Member owner, String partyName, List<Member> members, String showcaseTitle, String showcaseDescription) {
        PartyDto party = partyService.create(owner, partyName, partyName + " 전시 테스트", "전시 기록을 확인하기 위한 개발 데이터입니다.", null, null, null, TopicType.PROJECT, PartyTag.WEB, null, LocalDateTime.now().plusDays(7), List.of(new PartyService.PositionCreateSpec(PositionType.BACK, members.size())));
        long positionId = party.positions().getFirst().id();
        for (Member member : members) {
            long applicationId = partyApplicationService.apply(party.id(), positionId, member, "함께하고 싶습니다.").id();
            partyApplicationService.decide(party.id(), applicationId, owner, PartyApplicationService.Decision.APPROVED);
        }
        partyLifecycleService.closeRecruiting(party.id(), owner);
        partyLifecycleService.complete(party.id(), owner);
        if (showcaseTitle != null) partyShowcaseService.publish(party.id(), owner, showcaseTitle, showcaseDescription);
    }

    private void createInProgressParty(
            Member owner,
            String partyName,
            String title,
            String githubRepoUrl,
            LocalDateTime deadline
    ) {
        PartyDto party = partyService.create(
                owner,
                partyName,
                title,
                "진행 중 파티의 GitHub App 설치와 Pull Request 동기화 흐름을 확인하기 위한 개발 데이터입니다.",
                null,
                null,
                null,
                TopicType.STUDY,
                PartyTag.WEB,
                githubRepoUrl,
                deadline,
                List.of(new PartyService.PositionCreateSpec(PositionType.BACK, 1))
        );
        partyLifecycleService.closeRecruiting(party.id(), owner);
    }

    private void apply(PartyDto party, PositionType type, Member applicant, String message) {
        long positionId = party.positions().stream().filter(p -> p.type() == type).findFirst().orElseThrow().id();
        partyApplicationService.apply(party.id(), positionId, applicant, message);
    }

    // 쪽지함 페이지 크기(20)를 넘기도록 user1 받은 쪽지를 24건 만든다. 오래된 절반은 읽음 처리.
    // user1 자리에 다른 계정을 넘기면 그 계정 기준으로 만든다.
    private void createMessages(Member user1, Member user2, Member user3) {
        String[] contents = {
                "안녕하세요! 파티 지원 관련해서 여쭤볼 게 있어요.",
                "이번 주 회의 시간 목요일 저녁 8시 괜찮으세요?",
                "PR 올렸습니다. 시간 되실 때 리뷰 부탁드려요.",
                "포트폴리오 링크 공유드립니다. 확인 부탁드려요!",
                "API 명세 초안 노션에 정리해 뒀어요.",
                "디자인 시안 두 가지 중에 어떤 게 나을까요?",
                "오늘 스터디 자료 올려두었습니다.",
                "배포 중에 에러가 나서 같이 봐주실 수 있나요?",
                "대회 제출 마감이 이번 주 일요일이에요. 일정 확인 부탁드립니다.",
                "지난번 말씀하신 레퍼런스 찾아서 보내드려요. 생각보다 길어져서 요약도 같이 적어뒀습니다. 핵심은 캐시 무효화 시점을 이벤트 기준으로 잡는 거예요."
        };

        List<Long> readIds = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Member sender = i % 2 == 0 ? user2 : user3;
            long id = messageService.send(sender, user1.getId(), contents[i % contents.length]).id();
            if (i < 12) readIds.add(id);
        }
        messageService.read(user1, readIds);

        for (int i = 0; i < 6; i++) {
            Member recipient = i % 2 == 0 ? user2 : user3;
            messageService.send(user1, recipient.getId(), contents[(i + 3) % contents.length]);
        }
        messageService.send(user2, user3.getId(), contents[1]);
        messageService.send(user3, user2.getId(), contents[2]);
    }

    private void createGoals(Member user1, Member user2, Member user3, LocalDate today) {
        goalService.createSelfReported(user1, contestGoal(
                GoalStatus.ACHIEVED, "2025 공공데이터 분석 경진대회", true,
                "우수상", today.minusMonths(3), "https://example.com/contests/public-data-2025"
        ));
        goalService.createSelfReported(user1, checklistGoal(
                GoalStatus.IN_PROGRESS, "Spring Boot 성능 최적화",
                "부하 테스트와 쿼리 튜닝을 실제 프로젝트에 적용하기", today.plusDays(30)
        ));
        goalService.createSelfReported(user2, checklistGoal(
                GoalStatus.WANT, "SQLD 자격증 취득", "주 3회 기출문제 풀이", today.plusMonths(2)
        ));
        goalService.createSelfReported(user2, contestGoal(
                GoalStatus.HOLD, "지역문제 해결 해커톤", false,
                "팀원 일정 조율 중", null, "https://example.com/contests/local-problem"
        ));
        goalService.createSelfReported(user3, checklistGoal(
                GoalStatus.ACHIEVED, "포트폴리오 리뉴얼",
                "프로젝트 3개와 디자인 시스템 정리 완료", today.minusDays(10)
        ));
    }

    private GoalCreateReqBody contestGoal(
            GoalStatus status, String contestName, boolean isTeam,
            String result, LocalDate awardDate, String contestUrl
    ) {
        return new GoalCreateReqBody(
                GoalType.CONTEST,
                status,
                new GoalDetailReqBody(
                        isTeam, result, awardDate, contestUrl,
                        null, null, null,
                        contestName, null, null, null
                )
        );
    }

    private GoalCreateReqBody checklistGoal(
            GoalStatus status, String title, String memo, LocalDate targetDate
    ) {
        return new GoalCreateReqBody(
                GoalType.CHECKLIST,
                status,
                new GoalDetailReqBody(
                        null, null, null, null,
                        null, null, null,
                        title, memo, targetDate, null
                )
        );
    }

    private void createNotifications(Member user1, Member user2) {
        Notification readNotification = notificationService.create(user1, NotificationType.PARTY_APPLICATION_APPROVED, "AI 크루 파티 참여 신청이 승인되었습니다.");
        readNotification.read();
        notificationService.create(user2, NotificationType.PARTY_APPLICATION_APPROVED, "스프링 스터디 파티 참여 신청이 승인되었습니다.");
    }
}
