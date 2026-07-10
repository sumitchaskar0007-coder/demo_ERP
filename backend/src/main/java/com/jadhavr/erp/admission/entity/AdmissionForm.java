package com.jadhavr.erp.admission.entity;

import com.jadhavr.erp.admission.enums.AdmissionSource;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "admission_forms")
public class AdmissionForm extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_reference_number", nullable = false, unique = true, length = 50)
    private String admissionReferenceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User studentUser;

    @Column(nullable = false, length = 20)
    private String academicYear;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(length = 80)
    private String middleName;

    @Column(nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 30)
    private String gender;

    @Column(length = 250)
    private String addressLine1;

    @Column(length = 250)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pincode;

    @Column(nullable = false, length = 150)
    private String parentName;

    @Column(nullable = false, length = 20)
    private String parentPhone;

    @Column(length = 150)
    private String parentEmail;

    @Column(length = 200)
    private String previousSchoolName;

    @Column(length = 100)
    private String previousClassName;

    @Column(precision = 5, scale = 2)
    private BigDecimal previousPercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdmissionStatus status = AdmissionStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdmissionSource source = AdmissionSource.PUBLIC_LINK;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime studentSectionVerifiedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_section_verified_by")
    private User studentSectionVerifiedBy;
    @Column(length = 500)
    private String studentSectionRemarks;
    private LocalDateTime studentSectionRejectedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_section_rejected_by")
    private User studentSectionRejectedBy;
    private LocalDateTime principalApprovedAt;

    @Column(length = 500)
    private String rejectionReason;

    private LocalDateTime lastPrintedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_printed_by")
    private User lastPrintedBy;
    @Column(nullable = false)
    private Integer printCount = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAdmissionReferenceNumber() { return admissionReferenceNumber; }
    public void setAdmissionReferenceNumber(String admissionReferenceNumber) { this.admissionReferenceNumber = admissionReferenceNumber; }
    public College getCollege() { return college; }
    public void setCollege(College college) { this.college = college; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public StudentProfile getStudent() { return student; }
    public void setStudent(StudentProfile student) { this.student = student; }
    public User getStudentUser() { return studentUser; }
    public void setStudentUser(User studentUser) { this.studentUser = studentUser; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }
    public String getParentEmail() { return parentEmail; }
    public void setParentEmail(String parentEmail) { this.parentEmail = parentEmail; }
    public String getPreviousSchoolName() { return previousSchoolName; }
    public void setPreviousSchoolName(String previousSchoolName) { this.previousSchoolName = previousSchoolName; }
    public String getPreviousClassName() { return previousClassName; }
    public void setPreviousClassName(String previousClassName) { this.previousClassName = previousClassName; }
    public BigDecimal getPreviousPercentage() { return previousPercentage; }
    public void setPreviousPercentage(BigDecimal previousPercentage) { this.previousPercentage = previousPercentage; }
    public AdmissionStatus getStatus() { return status; }
    public void setStatus(AdmissionStatus status) { this.status = status; }
    public AdmissionSource getSource() { return source; }
    public void setSource(AdmissionSource source) { this.source = source; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getStudentSectionVerifiedAt() { return studentSectionVerifiedAt; }
    public void setStudentSectionVerifiedAt(LocalDateTime studentSectionVerifiedAt) { this.studentSectionVerifiedAt = studentSectionVerifiedAt; }
    public User getStudentSectionVerifiedBy() { return studentSectionVerifiedBy; }
    public void setStudentSectionVerifiedBy(User studentSectionVerifiedBy) { this.studentSectionVerifiedBy = studentSectionVerifiedBy; }
    public String getStudentSectionRemarks() { return studentSectionRemarks; }
    public void setStudentSectionRemarks(String studentSectionRemarks) { this.studentSectionRemarks = studentSectionRemarks; }
    public LocalDateTime getStudentSectionRejectedAt() { return studentSectionRejectedAt; }
    public void setStudentSectionRejectedAt(LocalDateTime studentSectionRejectedAt) { this.studentSectionRejectedAt = studentSectionRejectedAt; }
    public User getStudentSectionRejectedBy() { return studentSectionRejectedBy; }
    public void setStudentSectionRejectedBy(User studentSectionRejectedBy) { this.studentSectionRejectedBy = studentSectionRejectedBy; }
    public LocalDateTime getPrincipalApprovedAt() { return principalApprovedAt; }
    public void setPrincipalApprovedAt(LocalDateTime principalApprovedAt) { this.principalApprovedAt = principalApprovedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getLastPrintedAt() { return lastPrintedAt; }
    public void setLastPrintedAt(LocalDateTime lastPrintedAt) { this.lastPrintedAt = lastPrintedAt; }
    public User getLastPrintedBy() { return lastPrintedBy; }
    public void setLastPrintedBy(User lastPrintedBy) { this.lastPrintedBy = lastPrintedBy; }
    public Integer getPrintCount() { return printCount; }
    public void setPrintCount(Integer printCount) { this.printCount = printCount; }
}
