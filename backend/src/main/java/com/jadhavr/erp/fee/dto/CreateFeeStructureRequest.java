package com.jadhavr.erp.fee.dto;
import com.jadhavr.erp.fee.enums.StudentCategory;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CreateFeeStructureRequest(@NotNull Long collegeId,@NotNull Long departmentId,@NotBlank @Size(max=20) String academicYear,@Size(max=150) String courseYear,@NotBlank @Size(min=2,max=150) String title,@Size(max=500) String description,@NotNull @Positive BigDecimal totalFee,@NotNull @PositiveOrZero BigDecimal minimumAmountForAdmission,@PositiveOrZero BigDecimal admissionFee,@PositiveOrZero BigDecimal tuitionFee,@PositiveOrZero BigDecimal examFee,@PositiveOrZero BigDecimal libraryFee,@PositiveOrZero BigDecimal otherFee,StudentCategory studentCategory,@NotBlank @Pattern(regexp="(?i)MALE|FEMALE") String gender,@Size(min=2,max=80) String customCategoryName,@NotNull @PositiveOrZero BigDecimal scholarshipAmount){public CreateFeeStructureRequest{if(courseYear==null||courseYear.isBlank())courseYear="First Year";}}
