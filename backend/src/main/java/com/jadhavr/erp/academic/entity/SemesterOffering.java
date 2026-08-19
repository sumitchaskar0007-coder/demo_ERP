package com.jadhavr.erp.academic.entity;

import com.jadhavr.erp.academic.entity.AcademicModels.AcademicTerm;
import com.jadhavr.erp.academic.entity.AcademicModels.AcademicYear;
import com.jadhavr.erp.academic.enums.SemesterOfferingStatus;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.department.entity.Department;
import jakarta.persistence.*;

@Entity
@Table(name = "semester_offerings", uniqueConstraints =
        @UniqueConstraint(columnNames = {"academic_term_id", "curriculum_semester_id"}))
public class SemesterOffering extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="college_id") private College college;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="department_id") private Department department;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="academic_year_id") private AcademicYear academicYear;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="academic_term_id") private AcademicTerm academicTerm;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="curriculum_semester_id") private CurriculumSemester curriculumSemester;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SemesterOfferingStatus status=SemesterOfferingStatus.PLANNED;
    public Long getId(){return id;} public College getCollege(){return college;} public void setCollege(College v){college=v;}
    public Department getDepartment(){return department;} public void setDepartment(Department v){department=v;}
    public AcademicYear getAcademicYear(){return academicYear;} public void setAcademicYear(AcademicYear v){academicYear=v;}
    public AcademicTerm getAcademicTerm(){return academicTerm;} public void setAcademicTerm(AcademicTerm v){academicTerm=v;}
    public CurriculumSemester getCurriculumSemester(){return curriculumSemester;} public void setCurriculumSemester(CurriculumSemester v){curriculumSemester=v;}
    public SemesterOfferingStatus getStatus(){return status;} public void setStatus(SemesterOfferingStatus v){status=v;}
}
