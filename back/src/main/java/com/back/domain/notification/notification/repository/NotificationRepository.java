package com.back.domain.notification.notification.repository;

import com.back.domain.notification.notification.entity.Notification;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByMemberAndIsRead(Member member, boolean isRead, Pageable pageable);

    Page<Notification> findByMember(Member member, Pageable pageable);

    List<Notification> findByMemberAndIdIn(Member member, Collection<Long> ids);
}
