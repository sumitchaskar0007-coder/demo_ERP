package com.collegeerp.erp.reports.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.reports")
public class ReportExportProperties {
    private boolean enabled = true;
    private boolean databasePollEnabled = true;
    private int pageSize = 500;
    private int maxRows = 100_000;
    private long maxOutputBytes = 50L * 1024 * 1024;
    private int maxAttempts = 3;
    private int maxActivePerUser = 3;
    private int batchSize = 2;
    private Duration retryDelay = Duration.ofSeconds(30);
    private Duration staleAfter = Duration.ofMinutes(20);
    private Duration resultTtl = Duration.ofHours(24);
    private Duration downloadExpiry = Duration.ofMinutes(2);
    private final Sqs sqs = new Sqs();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDatabasePollEnabled() {
        return databasePollEnabled;
    }

    public void setDatabasePollEnabled(boolean databasePollEnabled) {
        this.databasePollEnabled = databasePollEnabled;
    }

    public int getPageSize() {
        return Math.max(50, Math.min(pageSize, 1_000));
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getMaxRows() {
        return Math.max(1_000, Math.min(maxRows, 500_000));
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public long getMaxOutputBytes() {
        return Math.max(1024L * 1024, Math.min(maxOutputBytes, 250L * 1024 * 1024));
    }

    public void setMaxOutputBytes(long maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    public int getMaxAttempts() {
        return Math.max(1, Math.min(maxAttempts, 10));
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getBatchSize() {
        return Math.max(1, Math.min(batchSize, 10));
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxActivePerUser() {
        return Math.max(1, Math.min(maxActivePerUser, 20));
    }

    public void setMaxActivePerUser(int maxActivePerUser) {
        this.maxActivePerUser = maxActivePerUser;
    }

    public Duration getRetryDelay() {
        return positive(retryDelay, Duration.ofSeconds(30));
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public Duration getStaleAfter() {
        return positive(staleAfter, Duration.ofMinutes(20));
    }

    public void setStaleAfter(Duration staleAfter) {
        this.staleAfter = staleAfter;
    }

    public Duration getResultTtl() {
        return positive(resultTtl, Duration.ofHours(24));
    }

    public void setResultTtl(Duration resultTtl) {
        this.resultTtl = resultTtl;
    }

    public Duration getDownloadExpiry() {
        Duration value = positive(downloadExpiry, Duration.ofMinutes(2));
        if (value.compareTo(Duration.ofSeconds(30)) < 0) return Duration.ofSeconds(30);
        return value.compareTo(Duration.ofMinutes(10)) > 0 ? Duration.ofMinutes(10) : value;
    }

    public void setDownloadExpiry(Duration downloadExpiry) {
        this.downloadExpiry = downloadExpiry;
    }

    public Sqs getSqs() {
        return sqs;
    }

    @PostConstruct
    void validateTransport() {
        if ((sqs.isProducerEnabled() || sqs.isConsumerEnabled()) && sqs.getQueueUrl().isBlank()) {
            throw new IllegalStateException(
                    "Report SQS producer/consumer requires app.reports.sqs.queue-url");
        }
        if (sqs.isConsumerEnabled() && !databasePollEnabled) {
            throw new IllegalStateException(
                    "Report SQS consumers require database polling for retry and stale-job recovery");
        }
        if (enabled
                && !databasePollEnabled
                && !sqs.isProducerEnabled()
                && !sqs.isConsumerEnabled()) {
            throw new IllegalStateException(
                    "Enabled report exports require database polling or an SQS producer/consumer");
        }
    }

    private static Duration positive(Duration value, Duration fallback) {
        return value == null || value.isZero() || value.isNegative() ? fallback : value;
    }

    public static class Sqs {
        private boolean producerEnabled;
        private boolean consumerEnabled;
        private String queueUrl = "";
        private int waitSeconds = 10;

        public boolean isProducerEnabled() {
            return producerEnabled;
        }

        public void setProducerEnabled(boolean producerEnabled) {
            this.producerEnabled = producerEnabled;
        }

        public boolean isConsumerEnabled() {
            return consumerEnabled;
        }

        public void setConsumerEnabled(boolean consumerEnabled) {
            this.consumerEnabled = consumerEnabled;
        }

        public String getQueueUrl() {
            return queueUrl == null ? "" : queueUrl.trim();
        }

        public void setQueueUrl(String queueUrl) {
            this.queueUrl = queueUrl;
        }

        public int getWaitSeconds() {
            return Math.max(1, Math.min(waitSeconds, 20));
        }

        public void setWaitSeconds(int waitSeconds) {
            this.waitSeconds = waitSeconds;
        }
    }
}
