package com.jadhavr.erp.email.config;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.InternetAddress;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    public enum Transport {
        DATABASE,
        SQS
    }

    private boolean enabled;
    private String fromAddress = "noreply@localhost";
    private String fromName = "Jadhavar Institute";
    private String replyTo = "noreply@localhost";
    private String configurationSet = "";
    private int maxRetries = 3;
    private int retryDelaySeconds = 60;
    private int batchSize = 20;
    private int tokenExpiryMinutes = 30;
    private Transport transport = Transport.DATABASE;
    private final Sqs sqs = new Sqs();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getConfigurationSet() {
        return configurationSet;
    }

    public void setConfigurationSet(String configurationSet) {
        this.configurationSet = configurationSet == null ? "" : configurationSet.trim();
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getTokenExpiryMinutes() {
        return tokenExpiryMinutes;
    }

    public void setTokenExpiryMinutes(int tokenExpiryMinutes) {
        this.tokenExpiryMinutes = tokenExpiryMinutes;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public boolean isDatabaseTransport() {
        return transport == Transport.DATABASE;
    }

    public boolean isSqsTransport() {
        return transport == Transport.SQS;
    }

    public Sqs getSqs() {
        return sqs;
    }

    @PostConstruct
    void validateTransportSelection() {
        if (transport != Transport.SQS
                && (sqs.isPublisherEnabled()
                        || sqs.isConsumerEnabled()
                        || sqs.isRecoveryEnabled())) {
            throw new IllegalStateException(
                    "Email SQS roles require app.mail.transport=sqs");
        }
        if (maxRetries < 1 || maxRetries > 10
                || retryDelaySeconds < 1 || retryDelaySeconds > 3_600
                || batchSize < 1 || batchSize > 100
                || tokenExpiryMinutes < 5 || tokenExpiryMinutes > 1_440) {
            throw new IllegalStateException("Mail retry, batch, or token limits are invalid");
        }
        if (!configurationSet.isEmpty()
                && !configurationSet.matches("^[A-Za-z0-9_-]{1,64}$")) {
            throw new IllegalStateException("SES configuration set name is invalid");
        }
        if (enabled) {
            requireAddress(fromAddress, "from address");
            requireAddress(replyTo, "reply-to address");
            if (fromName == null || fromName.isBlank() || fromName.length() > 100
                    || fromName.indexOf('\r') >= 0 || fromName.indexOf('\n') >= 0) {
                throw new IllegalStateException("Mail sender name is invalid");
            }
        }
    }

    private void requireAddress(String value, String label) {
        try {
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
            if (!address.getAddress().contains("@")) throw new IllegalArgumentException();
        } catch (Exception exception) {
            throw new IllegalStateException("Mail " + label + " is invalid", exception);
        }
    }

    public static class Sqs {
        private String queueUrl;
        private boolean publisherEnabled;
        private boolean consumerEnabled;
        private boolean recoveryEnabled;
        private int waitTimeSeconds = 20;
        private int visibilityTimeoutSeconds = 120;
        private int maxMessages = 10;
        private long pollDelayMs = 1_000;
        private long recoveryDelayMs = 60_000;
        private int recoveryAgeSeconds = 60;
        private int recoveryBatchSize = 100;

        public String getQueueUrl() {
            return queueUrl;
        }

        public void setQueueUrl(String queueUrl) {
            this.queueUrl = queueUrl;
        }

        public boolean isPublisherEnabled() {
            return publisherEnabled;
        }

        public void setPublisherEnabled(boolean publisherEnabled) {
            this.publisherEnabled = publisherEnabled;
        }

        public boolean isConsumerEnabled() {
            return consumerEnabled;
        }

        public void setConsumerEnabled(boolean consumerEnabled) {
            this.consumerEnabled = consumerEnabled;
        }

        public boolean isRecoveryEnabled() {
            return recoveryEnabled;
        }

        public void setRecoveryEnabled(boolean recoveryEnabled) {
            this.recoveryEnabled = recoveryEnabled;
        }

        public int getWaitTimeSeconds() {
            return waitTimeSeconds;
        }

        public void setWaitTimeSeconds(int waitTimeSeconds) {
            this.waitTimeSeconds = waitTimeSeconds;
        }

        public int getVisibilityTimeoutSeconds() {
            return visibilityTimeoutSeconds;
        }

        public void setVisibilityTimeoutSeconds(int visibilityTimeoutSeconds) {
            this.visibilityTimeoutSeconds = visibilityTimeoutSeconds;
        }

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }

        public long getPollDelayMs() {
            return pollDelayMs;
        }

        public void setPollDelayMs(long pollDelayMs) {
            this.pollDelayMs = pollDelayMs;
        }

        public long getRecoveryDelayMs() {
            return recoveryDelayMs;
        }

        public void setRecoveryDelayMs(long recoveryDelayMs) {
            this.recoveryDelayMs = recoveryDelayMs;
        }

        public int getRecoveryAgeSeconds() {
            return recoveryAgeSeconds;
        }

        public void setRecoveryAgeSeconds(int recoveryAgeSeconds) {
            this.recoveryAgeSeconds = recoveryAgeSeconds;
        }

        public int getRecoveryBatchSize() {
            return recoveryBatchSize;
        }

        public void setRecoveryBatchSize(int recoveryBatchSize) {
            this.recoveryBatchSize = recoveryBatchSize;
        }
    }
}
