package com.back.global.github.repository;

import com.back.global.github.entity.GithubWebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

/** 웹훅 본문 처리 전에 delivery가 이미 처리됐는지 빠르게 확인한다. */
public interface GithubWebhookDeliveryRepository extends JpaRepository<GithubWebhookDelivery, Long> {
    boolean existsByDeliveryId(String deliveryId);
}
