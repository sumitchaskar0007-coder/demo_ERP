package com.jadhavr.erp.admission.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AdmissionDetailDraftRequest(
        @NotNull JsonNode values,
        @NotNull @PositiveOrZero Long version) {}
