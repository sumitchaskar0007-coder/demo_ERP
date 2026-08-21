package com.collegeerp.erp.email.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class EmailQueueMessageCodec {

    private final ObjectMapper objectMapper;

    public EmailQueueMessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(long notificationId) {
        if (notificationId < 1) {
            throw new IllegalArgumentException("Email notification ID must be positive");
        }
        return "{\"notificationId\":" + notificationId + "}";
    }

    public long decode(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || !root.isObject() || root.size() != 1) {
                throw new IllegalArgumentException("Invalid email queue message");
            }
            JsonNode id = root.get("notificationId");
            if (id == null || !id.canConvertToLong() || !id.isIntegralNumber()) {
                throw new IllegalArgumentException("Invalid email queue message");
            }
            long notificationId = id.longValue();
            if (notificationId < 1) {
                throw new IllegalArgumentException("Invalid email queue message");
            }
            return notificationId;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid email queue message", exception);
        }
    }
}
