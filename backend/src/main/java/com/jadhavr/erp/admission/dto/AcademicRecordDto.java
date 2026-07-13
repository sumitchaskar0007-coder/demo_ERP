package com.jadhavr.erp.admission.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AcademicRecordDto(
        @NotBlank @Pattern(regexp = "10TH|12TH|DIPLOMA|GRADUATION") String qualification,
        @Size(max = 200) String instituteName,
        @Size(max = 150) String boardUniversity,
        @Pattern(regexp = "^$|^[0-9]{4}$", message = "Year must contain four digits") String yearOfPassing,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal marksPercentage
) {}
