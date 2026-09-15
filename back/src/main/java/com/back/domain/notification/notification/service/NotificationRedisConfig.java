package com.back.domain.notification.notification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.ChannelTopic;

@Configuration
@ConditionalOnProperty(prefix = "custom.notification.redis-pubsub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRedisConfig {

    @Bean
    public RedisMessageListenerContainer notificationRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            NotificationRedisSubscriber notificationRedisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(notificationRedisSubscriber, new ChannelTopic(NotificationRedisPublisher.CHANNEL));
        return container;
    }
}
