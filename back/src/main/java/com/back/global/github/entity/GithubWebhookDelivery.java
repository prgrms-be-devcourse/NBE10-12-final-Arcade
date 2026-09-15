package com.back.global.github.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
/* GitHub의 X-GitHub-Delivery 값을 저장해 webhook 재전송을 idempotent하게 만든다. */
public class GithubWebhookDelivery extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String deliveryId;

    public GithubWebhookDelivery(String deliveryId) {
        this.deliveryId = deliveryId;
    }
}
