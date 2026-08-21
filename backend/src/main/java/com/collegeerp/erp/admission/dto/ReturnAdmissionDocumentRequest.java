package com.collegeerp.erp.admission.dto;
import jakarta.validation.constraints.Size;
public record ReturnAdmissionDocumentRequest(boolean returnedToStudent,
        @Size(max=500) String remarks) {}
