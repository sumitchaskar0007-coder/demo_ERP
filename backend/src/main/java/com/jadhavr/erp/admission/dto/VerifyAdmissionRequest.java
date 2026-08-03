package com.jadhavr.erp.admission.dto;

import com.jadhavr.erp.fee.enums.StudentCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

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
        @Size(max = 50) List<@Valid DocumentCustody> documentCustody,
        @Size(max = 500) String remarks,
        @Size(min = 2, max = 80) String customCategoryName
) {
    public VerifyAdmissionRequest(StudentCategory studentCategory, Boolean photoVerified,
            Boolean tenthMarksheetVerified, Boolean twelfthMarksheetVerified,
            Boolean leavingCertificateVerified, Boolean aadhaarCardVerified,
            Boolean graduationPgCertificateVerified, Boolean migrationCertificateVerified,
            Boolean gapAffidavitVerified, Boolean casteCertificateVerified,
            Boolean incomeProofVerified, Boolean nameChangeCertificateVerified,
            List<DocumentCustody> documentCustody, String remarks) {
        this(studentCategory, photoVerified, tenthMarksheetVerified, twelfthMarksheetVerified,
                leavingCertificateVerified, aadhaarCardVerified, graduationPgCertificateVerified,
                migrationCertificateVerified, gapAffidavitVerified, casteCertificateVerified,
                incomeProofVerified, nameChangeCertificateVerified, documentCustody, remarks, null);
    }
    public record DocumentCustody(
            @NotBlank
            @Size(max = 80)
            @Pattern(regexp = "^[A-Z][A-Z0-9_]*$",
                    message = "Document type must be an uppercase schema key")
            String documentType,
            boolean originalReceived,
            boolean xeroxReceived) {}
}
