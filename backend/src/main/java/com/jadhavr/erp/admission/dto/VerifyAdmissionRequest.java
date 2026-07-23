package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.fee.enums.StudentCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyAdmissionRequest(
        @NotNull StudentCategory studentCategory,
        @NotNull Boolean photoVerified,
        @NotNull Boolean tenthMarksheetVerified,
        @NotNull Boolean twelfthMarksheetVerified,
        @NotNull Boolean leavingCertificateVerified,
        @NotNull Boolean aadhaarCardVerified,
        @NotNull Boolean graduationPgCertificateVerified,
        @NotNull Boolean migrationCertificateVerified,
        @NotNull Boolean gapAffidavitVerified,
        @NotNull Boolean casteCertificateVerified,
        @NotNull Boolean incomeProofVerified,
        @NotNull Boolean nameChangeCertificateVerified,
        @Size(max = 500) String remarks
) {
}
