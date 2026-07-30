package com.jadhavr.erp.notice.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "app.rate-limit.redis-enabled", havingValue = "true")
public class NoticeStreamConfiguration {
    @Bean
    RedisMessageListenerContainer noticeRedisListener(
            RedisConnectionFactory factory, NoticeStreamService streams) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener((message, pattern) ->
                        streams.onSharedEvent(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(NoticeStreamService.CHANNEL));
        return container;
    }
}
