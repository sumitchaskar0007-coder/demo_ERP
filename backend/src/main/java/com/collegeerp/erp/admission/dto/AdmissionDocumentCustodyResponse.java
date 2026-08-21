package com.collegeerp.erp.admission.dto;
import java.time.LocalDateTime;
public record AdmissionDocumentCustodyResponse(String documentType, boolean originalReceived,
        boolean xeroxReceived, LocalDateTime receivedAt, boolean returnedToStudent,
        LocalDateTime returnedAt, String returnRemarks) {}
