package com.back.domain.notification.notification.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.notification.notification.dtos.NotificationDto;
import com.back.domain.notification.notification.dtos.NotificationPageDto;
import com.back.domain.notification.notification.dtos.NotificationReadResponse;
import com.back.domain.notification.notification.entity.Notification;
import com.back.domain.notification.notification.entity.NotificationType;
import com.back.domain.notification.notification.repository.NotificationRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;

    /**
     * 알림을 발행하는 도메인 서비스가 공통으로 사용하는 생성 진입점이다.
     * 알림의 읽음/삭제 정책은 이 서비스에서 계속 관리한다.
     */
    @Transactional
    public Notification create(Member member, NotificationType type, String content) {
        return notificationRepository.save(new Notification(member, type, content));
    }

    public NotificationPageDto getList(Member member, Boolean isRead, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createDate"),
                Sort.Order.desc("id")
        ));

        Page<Notification> notifications = isRead == null
                ? notificationRepository.findByMember(member, pageable)
                : notificationRepository.findByMemberAndIsRead(member, isRead, pageable);

        return new NotificationPageDto(notifications.map(NotificationDto::new));
    }

    @Transactional
    public List<NotificationReadResponse> read(Member member, List<Long> ids) {
        Map<Long, Notification> notificationsById = notificationsById(member, ids, true);

        return ids.stream()
                .map(notificationsById::get)
                .peek(Notification::read)
                .map(NotificationReadResponse::new)
                .toList();
    }

    @Transactional
    public void delete(Member member, List<Long> ids) {
        // 삭제는 idempotent하게 처리한다. 이미 삭제됐거나 다른 회원의 알림은 삭제 대상에서 제외한다.
        notificationRepository.deleteAll(notificationsById(member, ids, false).values());
    }

    private Map<Long, Notification> notificationsById(Member member, List<Long> ids, boolean requireAll) {
        Set<Long> uniqueIds = new HashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            throw new ServiceException("400-1", "ids에 중복된 알림이 포함되어 있습니다.");
        }

        List<Notification> notifications = notificationRepository.findByMemberAndIdIn(member, uniqueIds);
        if (requireAll && notifications.size() != uniqueIds.size()) {
            throw new ServiceException("404-1", "존재하지 않는 알림입니다.");
        }

        Map<Long, Notification> result = new HashMap<>();
        for (Notification notification : notifications) {
            result.put(notification.getId(), notification);
        }
        return result;
    }
}
