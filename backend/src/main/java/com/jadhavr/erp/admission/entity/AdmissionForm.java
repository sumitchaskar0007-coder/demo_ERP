package com.jadhavr.erp.admission.entity;

import com.jadhavr.erp.admission.enums.AdmissionSource;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.fee.enums.StudentCategory;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.OrderColumn;
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
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'OPEN'")
    private StudentCategory studentCategory = StudentCategory.OPEN;

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
    @Column(length = 180)
    private String photoStorageName;

    @Column(length = 180)
    private String tenthMarksheetStorageName;

    @Column(length = 180)
    private String twelfthMarksheetStorageName;

    @Column(length = 180)
    private String graduationPgCertificateStorageName;
    @Column(length = 180)
    private String leavingCertificateStorageName;
    @Column(length = 180)
    private String migrationCertificateStorageName;
    @Column(length = 180)
    private String gapAffidavitStorageName;
    @Column(length = 180)
    private String casteCertificateStorageName;
    @Column(length = 180)
    private String incomeProofStorageName;
    @Column(length = 180)
    private String nameChangeCertificateStorageName;
    @Column(length = 180)
    private String aadhaarCardStorageName;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean photoVerified = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean tenthMarksheetVerified = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean twelfthMarksheetVerified = false;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean leavingCertificateVerified = false;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean aadhaarCardVerified = false;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean graduationPgCertificateVerified = false;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean migrationCertificateVerified = false;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean gapAffidavitVerified = false;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean casteCertificateVerified = false;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean incomeProofVerified = false;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean nameChangeCertificateVerified = false;

    @Column(length = 120)
    private String placeOfBirth;

    @Column(length = 30)
    private String maritalStatus;

    @Column(length = 12, unique = true)
    private String aadhaarNumber;

    @Column(length = 30)
    private String apaarId;

    @Column(length = 80)
    private String nationality;

    @Column(length = 80)
    private String religion;

    @Column(length = 100)
    private String caste;

    @Column(length = 20)
    private String permanentPhone;

    @Column(length = 150)
    private String permanentEmail;

    @Column(length = 500)
    private String correspondenceAddress;

    @Column(length = 100)
    private String correspondenceCity;

    @Column(length = 10)
    private String correspondencePincode;

    @Column(length = 100)
    private String correspondenceState;

    @Column(length = 20)
    private String correspondencePhone;

    @Column(length = 20)
    private String correspondenceMobile;

    @Column(length = 150)
    private String correspondenceEmail;

    @Column(length = 80)
    private String qualifyingEntranceSeatNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal qualifyingEntranceTotalScore;

    @Column(length = 200)
    private String lastGraduationCollegeName;

    @Column(length = 500)
    private String lastGraduationCollegeAddress;

    private LocalDateTime detailsCompletedAt;

    @ElementCollection
    @CollectionTable(name = "admission_academic_records", joinColumns = @JoinColumn(name = "admission_form_id"))
    @OrderColumn(name = "record_order")
    private List<AdmissionAcademicRecord> academicRecords = new ArrayList<>();

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
    public StudentCategory getStudentCategory() { return studentCategory; }
    public void setStudentCategory(StudentCategory studentCategory) { this.studentCategory = studentCategory; }
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
    public String getPhotoStorageName() { return photoStorageName; }
    public void setPhotoStorageName(String photoStorageName) { this.photoStorageName = photoStorageName; }
    public String getTenthMarksheetStorageName() { return tenthMarksheetStorageName; }
    public void setTenthMarksheetStorageName(String value) { this.tenthMarksheetStorageName = value; }
    public String getTwelfthMarksheetStorageName() { return twelfthMarksheetStorageName; }
    public void setTwelfthMarksheetStorageName(String value) { this.twelfthMarksheetStorageName = value; }
    public String getGraduationPgCertificateStorageName() { return graduationPgCertificateStorageName; }
    public void setGraduationPgCertificateStorageName(String value) { this.graduationPgCertificateStorageName = value; }
    public String getLeavingCertificateStorageName() { return leavingCertificateStorageName; }
    public void setLeavingCertificateStorageName(String value) { this.leavingCertificateStorageName = value; }
    public String getMigrationCertificateStorageName() { return migrationCertificateStorageName; }
    public void setMigrationCertificateStorageName(String value) { this.migrationCertificateStorageName = value; }
    public String getGapAffidavitStorageName() { return gapAffidavitStorageName; }
    public void setGapAffidavitStorageName(String value) { this.gapAffidavitStorageName = value; }
    public String getCasteCertificateStorageName() { return casteCertificateStorageName; }
    public void setCasteCertificateStorageName(String value) { this.casteCertificateStorageName = value; }
    public String getIncomeProofStorageName() { return incomeProofStorageName; }
    public void setIncomeProofStorageName(String value) { this.incomeProofStorageName = value; }
    public String getNameChangeCertificateStorageName() { return nameChangeCertificateStorageName; }
    public void setNameChangeCertificateStorageName(String value) { this.nameChangeCertificateStorageName = value; }
    public String getAadhaarCardStorageName() { return aadhaarCardStorageName; }
    public void setAadhaarCardStorageName(String value) { this.aadhaarCardStorageName = value; }
    public boolean isPhotoVerified() { return photoVerified; }
    public void setPhotoVerified(boolean value) { this.photoVerified = value; }
    public boolean isTenthMarksheetVerified() { return tenthMarksheetVerified; }
    public void setTenthMarksheetVerified(boolean value) { this.tenthMarksheetVerified = value; }
    public boolean isTwelfthMarksheetVerified() { return twelfthMarksheetVerified; }
    public void setTwelfthMarksheetVerified(boolean value) { this.twelfthMarksheetVerified = value; }
    public boolean isLeavingCertificateVerified() { return leavingCertificateVerified; }
    public void setLeavingCertificateVerified(boolean value) { this.leavingCertificateVerified = value; }
    public boolean isAadhaarCardVerified() { return aadhaarCardVerified; }
    public void setAadhaarCardVerified(boolean value) { this.aadhaarCardVerified = value; }
    public boolean isGraduationPgCertificateVerified() { return graduationPgCertificateVerified; }
    public void setGraduationPgCertificateVerified(boolean value) { this.graduationPgCertificateVerified = value; }
    public boolean isMigrationCertificateVerified() { return migrationCertificateVerified; }
    public void setMigrationCertificateVerified(boolean value) { this.migrationCertificateVerified = value; }
    public boolean isGapAffidavitVerified() { return gapAffidavitVerified; }
    public void setGapAffidavitVerified(boolean value) { this.gapAffidavitVerified = value; }
    public boolean isCasteCertificateVerified() { return casteCertificateVerified; }
    public void setCasteCertificateVerified(boolean value) { this.casteCertificateVerified = value; }
    public boolean isIncomeProofVerified() { return incomeProofVerified; }
    public void setIncomeProofVerified(boolean value) { this.incomeProofVerified = value; }
    public boolean isNameChangeCertificateVerified() { return nameChangeCertificateVerified; }
    public void setNameChangeCertificateVerified(boolean value) { this.nameChangeCertificateVerified = value; }
    public String getPlaceOfBirth() { return placeOfBirth; }
    public void setPlaceOfBirth(String placeOfBirth) { this.placeOfBirth = placeOfBirth; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }
    public String getApaarId() { return apaarId; }
    public void setApaarId(String apaarId) { this.apaarId = apaarId; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }
    public String getCaste() { return caste; }
    public void setCaste(String caste) { this.caste = caste; }
    public String getPermanentPhone() { return permanentPhone; }
    public void setPermanentPhone(String permanentPhone) { this.permanentPhone = permanentPhone; }
    public String getPermanentEmail() { return permanentEmail; }
    public void setPermanentEmail(String permanentEmail) { this.permanentEmail = permanentEmail; }
    public String getCorrespondenceAddress() { return correspondenceAddress; }
    public void setCorrespondenceAddress(String correspondenceAddress) { this.correspondenceAddress = correspondenceAddress; }
    public String getCorrespondenceCity() { return correspondenceCity; }
    public void setCorrespondenceCity(String correspondenceCity) { this.correspondenceCity = correspondenceCity; }
    public String getCorrespondencePincode() { return correspondencePincode; }
    public void setCorrespondencePincode(String correspondencePincode) { this.correspondencePincode = correspondencePincode; }
    public String getCorrespondenceState() { return correspondenceState; }
    public void setCorrespondenceState(String correspondenceState) { this.correspondenceState = correspondenceState; }
    public String getCorrespondencePhone() { return correspondencePhone; }
    public void setCorrespondencePhone(String correspondencePhone) { this.correspondencePhone = correspondencePhone; }
    public String getCorrespondenceMobile() { return correspondenceMobile; }
    public void setCorrespondenceMobile(String correspondenceMobile) { this.correspondenceMobile = correspondenceMobile; }
    public String getCorrespondenceEmail() { return correspondenceEmail; }
    public void setCorrespondenceEmail(String correspondenceEmail) { this.correspondenceEmail = correspondenceEmail; }
    public String getQualifyingEntranceSeatNumber() { return qualifyingEntranceSeatNumber; }
    public void setQualifyingEntranceSeatNumber(String qualifyingEntranceSeatNumber) { this.qualifyingEntranceSeatNumber = qualifyingEntranceSeatNumber; }
    public BigDecimal getQualifyingEntranceTotalScore() { return qualifyingEntranceTotalScore; }
    public void setQualifyingEntranceTotalScore(BigDecimal qualifyingEntranceTotalScore) { this.qualifyingEntranceTotalScore = qualifyingEntranceTotalScore; }
    public String getLastGraduationCollegeName() { return lastGraduationCollegeName; }
    public void setLastGraduationCollegeName(String lastGraduationCollegeName) { this.lastGraduationCollegeName = lastGraduationCollegeName; }
    public String getLastGraduationCollegeAddress() { return lastGraduationCollegeAddress; }
    public void setLastGraduationCollegeAddress(String lastGraduationCollegeAddress) { this.lastGraduationCollegeAddress = lastGraduationCollegeAddress; }
    public LocalDateTime getDetailsCompletedAt() { return detailsCompletedAt; }
    public void setDetailsCompletedAt(LocalDateTime detailsCompletedAt) { this.detailsCompletedAt = detailsCompletedAt; }
    public List<AdmissionAcademicRecord> getAcademicRecords() { return academicRecords; }
    public void setAcademicRecords(List<AdmissionAcademicRecord> academicRecords) { this.academicRecords = academicRecords; }
}
