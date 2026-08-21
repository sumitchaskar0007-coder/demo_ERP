package com.collegeerp.erp.notice.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class NoticeStreamConfiguration {

    @Bean(name = "noticeStreamExecutor")
    TaskExecutor noticeStreamExecutor(
            @Value("${app.notice.max-connections:10000}") int maxConnections) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        // One heartbeat fan-out must fit without dropping most connections
        // while only the minimum number of backend replicas are running.
        executor.setQueueCapacity(Math.max(2_000, maxConnections));
        executor.setThreadNamePrefix("notice-sse-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnProperty(name = "app.notice.redis-enabled", havingValue = "true")
    RedisMessageListenerContainer noticeRedisListener(
            RedisConnectionFactory factory, NoticeStreamService streams) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener((message, pattern) ->
                        streams.onSharedEvent(new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8)),
                new ChannelTopic(NoticeStreamService.CHANNEL));
        return container;
    }
}
