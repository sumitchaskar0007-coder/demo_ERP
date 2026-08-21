package com.collegeerp.erp.academic.entity;

import com.collegeerp.erp.academic.enums.SectionStatus;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.staff.entity.StaffProfile;
import jakarta.persistence.*;

@Entity
@Table(name = "course_year_divisions", uniqueConstraints =
        @UniqueConstraint(columnNames = {"academic_class_id", "academic_year", "code"}))
public class Section extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "college_id", nullable = false) private College college;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_class_id", nullable = false) private AcademicClass academicClass;
    @Column(name = "academic_year", nullable = false, length = 20) private String academicYear;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, length = 30) private String code;
    @Column(nullable = false) private Integer capacity;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_teacher_id") private StaffProfile classTeacher;
    @Enumerated(EnumType.STRING) private SectionStatus status = SectionStatus.ACTIVE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public College getCollege() { return college; }
    public void setCollege(College college) { this.college = college; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public AcademicClass getAcademicClass() { return academicClass; }
    public void setAcademicClass(AcademicClass academicClass) { this.academicClass = academicClass; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public StaffProfile getClassTeacher() { return classTeacher; }
    public void setClassTeacher(StaffProfile classTeacher) { this.classTeacher = classTeacher; }
    public SectionStatus getStatus() { return status; }
    public void setStatus(SectionStatus status) { this.status = status; }
}
