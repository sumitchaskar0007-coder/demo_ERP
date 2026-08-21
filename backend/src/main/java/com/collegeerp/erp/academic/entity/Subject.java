package com.collegeerp.erp.academic.entity;

import com.collegeerp.erp.academic.enums.SubjectStatus;
import com.collegeerp.erp.academic.enums.SubjectType;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.department.entity.Department;
import jakarta.persistence.*;

@Entity
@Table(name = "course_year_subjects")
public class Subject extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "college_id", nullable = false) private College college;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id", nullable = false) private Department department;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_class_id", nullable = false) private AcademicClass academicClass;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "curriculum_semester_id") private CurriculumSemester curriculumSemester;
    @Column(name = "academic_year", nullable = false, length = 20) private String academicYear;
    @Column(nullable = false, length = 150) private String name;
    @Column(nullable = false, length = 30) private String code;
    @Column(length = 500) private String description;
    private Integer credits;
    @Enumerated(EnumType.STRING) private SubjectStatus status = SubjectStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(length = 20) private SubjectType subjectType;

    public Long getId() { return id; }
    public College getCollege() { return college; }
    public void setCollege(College value) { college = value; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department value) { department = value; }
    public AcademicClass getAcademicClass() { return academicClass; }
    public void setAcademicClass(AcademicClass value) { academicClass = value; }
    public CurriculumSemester getCurriculumSemester() { return curriculumSemester; }
    public void setCurriculumSemester(CurriculumSemester value) { curriculumSemester = value; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String value) { academicYear = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer value) { credits = value; }
    public SubjectStatus getStatus() { return status; }
    public void setStatus(SubjectStatus value) { status = value; }
    public SubjectType getSubjectType() { return subjectType; }
    public void setSubjectType(SubjectType value) { subjectType = value; }
}
