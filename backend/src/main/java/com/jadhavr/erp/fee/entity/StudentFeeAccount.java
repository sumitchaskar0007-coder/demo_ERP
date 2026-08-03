package com.jadhavr.erp.fee.entity;

import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.fee.enums.FeeAccountStatus;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "student_fee_accounts")
public class StudentFeeAccount extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false) private StudentProfile student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_user_id", nullable = false) private User studentUser;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "admission_form_id", nullable = false) private AdmissionForm admissionForm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "college_id", nullable = false) private College college;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "fee_structure_id") private FeeStructure feeStructure;
    @Column(nullable = false, length = 20) private String academicYear;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'OPEN'") private StudentCategory studentCategory = StudentCategory.OPEN;
    @Column(name = "custom_category_name", length = 80) private String customCategoryName;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal totalFee;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal remainingAmount;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal minimumAmountForAdmission;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25) private FeeAccountStatus status = FeeAccountStatus.PENDING;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public StudentProfile getStudent() { return student; }
    public void setStudent(StudentProfile value) { student = value; }
    public User getStudentUser() { return studentUser; }
    public void setStudentUser(User value) { studentUser = value; }
    public AdmissionForm getAdmissionForm() { return admissionForm; }
    public void setAdmissionForm(AdmissionForm value) { admissionForm = value; }
    public College getCollege() { return college; }
    public void setCollege(College value) { college = value; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department value) { department = value; }
    public FeeStructure getFeeStructure() { return feeStructure; }
    public void setFeeStructure(FeeStructure value) { feeStructure = value; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String value) { academicYear = value; }
    public StudentCategory getStudentCategory() { return studentCategory; }
    public void setStudentCategory(StudentCategory value) { studentCategory = value; }
    public String getCustomCategoryName() { return customCategoryName; }
    public void setCustomCategoryName(String value) { customCategoryName = value; }
    public BigDecimal getTotalFee() { return totalFee; }
    public void setTotalFee(BigDecimal value) { totalFee = value; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal value) { paidAmount = value; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal value) { remainingAmount = value; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal value) { discountAmount = value; }
    public BigDecimal getMinimumAmountForAdmission() { return minimumAmountForAdmission; }
    public void setMinimumAmountForAdmission(BigDecimal value) { minimumAmountForAdmission = value; }
    public FeeAccountStatus getStatus() { return status; }
    public void setStatus(FeeAccountStatus value) { status = value; }
}
