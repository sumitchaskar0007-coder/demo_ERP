package com.collegeerp.erp.academic.entity;

import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.enums.CourseYearName;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.department.entity.Department;
import jakarta.persistence.*;

@Entity @Table(name="course_years", uniqueConstraints={
 @UniqueConstraint(name="uk_academic_class_code",columnNames={"college_id","department_id","academic_year","code"}),
 @UniqueConstraint(name="uk_course_year_name",columnNames={"college_id","department_id","academic_year","year_name"})})
public class AcademicClass extends BaseAuditEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="college_id") private College college;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="department_id") private Department department;
 @Column(name="academic_year",nullable=false,length=20) private String academicYear;
 @Enumerated(EnumType.STRING) @Column(name="year_name",nullable=false,length=30,columnDefinition="varchar(30) default 'FIRST_YEAR'") private CourseYearName yearName=CourseYearName.FIRST_YEAR;
 @Column(nullable=false,length=150) private String name; @Column(nullable=false,length=30) private String code;
 @Column(length=500) private String description; @Enumerated(EnumType.STRING) @Column(nullable=false) private AcademicStatus status=AcademicStatus.ACTIVE;
 public Long getId(){return id;} public void setId(Long v){id=v;} public College getCollege(){return college;} public void setCollege(College v){college=v;} public Department getDepartment(){return department;} public void setDepartment(Department v){department=v;} public String getAcademicYear(){return academicYear;} public void setAcademicYear(String v){academicYear=v;} public CourseYearName getYearName(){return yearName;} public void setYearName(CourseYearName v){yearName=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getCode(){return code;} public void setCode(String v){code=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public AcademicStatus getStatus(){return status;} public void setStatus(AcademicStatus v){status=v;}
}
