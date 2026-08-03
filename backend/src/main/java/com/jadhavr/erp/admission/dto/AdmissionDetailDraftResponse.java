package com.jadhavr.erp.admission.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record AdmissionDetailDraftResponse(JsonNode values, long version, LocalDateTime updatedAt) {}
