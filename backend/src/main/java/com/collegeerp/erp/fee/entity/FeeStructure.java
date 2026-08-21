package com.collegeerp.erp.fee.entity;

import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.fee.enums.FeeStructureStatus;
import com.collegeerp.erp.fee.enums.FeeBillingCycle;
import com.collegeerp.erp.fee.enums.StudentCategory;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "fee_structures")
public class FeeStructure extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "college_id", nullable = false) private College college;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @Column(nullable = false, length = 20) private String academicYear;
    @Column(name = "course_year", length = 150) private String courseYear;
    @Enumerated(EnumType.STRING) @Column(name="billing_cycle",nullable=false,length=20) private FeeBillingCycle billingCycle=FeeBillingCycle.ANNUAL;
    @Enumerated(EnumType.STRING) @Column(name = "student_category", nullable = false, length = 20) private StudentCategory studentCategory = StudentCategory.OPEN;
    @Column(name = "custom_category_name", length = 80) private String customCategoryName;
    @Column(length = 10) private String gender;
    @Column(nullable = false, length = 150) private String title;
    @Column(length = 500) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalFee;
    @Column(name = "scholarship_amount", nullable = false, precision = 12, scale = 2) private BigDecimal scholarshipAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal minimumAmountForAdmission;
    @Column(precision = 12, scale = 2) private BigDecimal admissionFee;
    @Column(precision = 12, scale = 2) private BigDecimal tuitionFee;
    @Column(precision = 12, scale = 2) private BigDecimal examFee;
    @Column(precision = 12, scale = 2) private BigDecimal libraryFee;
    @Column(precision = 12, scale = 2) private BigDecimal otherFee;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private FeeStructureStatus status = FeeStructureStatus.ACTIVE;
    public Long getId(){return id;} public void setId(Long v){id=v;} public long getVersion(){return version;} public void setVersion(long v){version=v;}
    public College getCollege(){return college;} public void setCollege(College v){college=v;} public Department getDepartment(){return department;} public void setDepartment(Department v){department=v;}
    public String getAcademicYear(){return academicYear;} public void setAcademicYear(String v){academicYear=v;} public String getCourseYear(){return courseYear;} public void setCourseYear(String v){courseYear=v;}
    public FeeBillingCycle getBillingCycle(){return billingCycle;} public void setBillingCycle(FeeBillingCycle v){billingCycle=v;}
    public StudentCategory getStudentCategory(){return studentCategory;} public void setStudentCategory(StudentCategory v){studentCategory=v;} public String getCustomCategoryName(){return customCategoryName;} public void setCustomCategoryName(String v){customCategoryName=v;} public String getGender(){return gender;} public void setGender(String v){gender=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public BigDecimal getTotalFee(){return totalFee;} public void setTotalFee(BigDecimal v){totalFee=v;} public BigDecimal getScholarshipAmount(){return scholarshipAmount;} public void setScholarshipAmount(BigDecimal v){scholarshipAmount=v;}
    public BigDecimal getMinimumAmountForAdmission(){return minimumAmountForAdmission;} public void setMinimumAmountForAdmission(BigDecimal v){minimumAmountForAdmission=v;}
    public BigDecimal getAdmissionFee(){return admissionFee;} public void setAdmissionFee(BigDecimal v){admissionFee=v;} public BigDecimal getTuitionFee(){return tuitionFee;} public void setTuitionFee(BigDecimal v){tuitionFee=v;} public BigDecimal getExamFee(){return examFee;} public void setExamFee(BigDecimal v){examFee=v;} public BigDecimal getLibraryFee(){return libraryFee;} public void setLibraryFee(BigDecimal v){libraryFee=v;} public BigDecimal getOtherFee(){return otherFee;} public void setOtherFee(BigDecimal v){otherFee=v;}
    public FeeStructureStatus getStatus(){return status;} public void setStatus(FeeStructureStatus v){status=v;}
}
