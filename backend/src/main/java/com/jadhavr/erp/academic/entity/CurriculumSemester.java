package com.jadhavr.erp.academic.entity;

import com.jadhavr.erp.academic.enums.AcademicTermType;
import com.jadhavr.erp.academic.enums.CourseYearName;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.department.entity.Department;
import jakarta.persistence.*;

@Entity
@Table(name = "curriculum_semesters", uniqueConstraints =
        @UniqueConstraint(name = "uk_curriculum_semester_department_number",
                columnNames = {"department_id", "semester_number"}))
public class CurriculumSemester extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "college_id") private College college;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "department_id") private Department department;
    @Column(name = "semester_number", nullable = false) private Integer semesterNumber;
    @Enumerated(EnumType.STRING) @Column(name = "year_name", nullable = false, length = 30) private CourseYearName yearName;
    @Enumerated(EnumType.STRING) @Column(name = "term_type", nullable = false, length = 10) private AcademicTermType termType;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false) private boolean active = true;

    public Long getId(){return id;} public College getCollege(){return college;} public void setCollege(College v){college=v;}
    public Department getDepartment(){return department;} public void setDepartment(Department v){department=v;}
    public Integer getSemesterNumber(){return semesterNumber;} public void setSemesterNumber(Integer v){semesterNumber=v;}
    public CourseYearName getYearName(){return yearName;} public void setYearName(CourseYearName v){yearName=v;}
    public AcademicTermType getTermType(){return termType;} public void setTermType(AcademicTermType v){termType=v;}
    public String getName(){return name;} public void setName(String v){name=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
