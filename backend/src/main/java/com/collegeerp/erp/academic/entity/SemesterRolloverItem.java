package com.collegeerp.erp.academic.entity;

import com.collegeerp.erp.academic.enums.SemesterRolloverDecision;
import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.student.entity.StudentProfile;
import jakarta.persistence.*;

@Entity @Table(name="semester_rollover_items",uniqueConstraints=@UniqueConstraint(columnNames={"job_id","student_id"}))
public class SemesterRolloverItem extends BaseAuditEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="job_id") private SemesterRolloverJob job;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="student_id") private StudentProfile student;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="source_enrollment_id") private StudentSectionEnrollment sourceEnrollment;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="target_enrollment_id") private StudentSectionEnrollment targetEnrollment;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SemesterRolloverDecision decision;
    @Column(length=500) private String message;
    public Long getId(){return id;} public SemesterRolloverJob getJob(){return job;} public void setJob(SemesterRolloverJob v){job=v;} public StudentProfile getStudent(){return student;} public void setStudent(StudentProfile v){student=v;} public StudentSectionEnrollment getSourceEnrollment(){return sourceEnrollment;} public void setSourceEnrollment(StudentSectionEnrollment v){sourceEnrollment=v;} public StudentSectionEnrollment getTargetEnrollment(){return targetEnrollment;} public void setTargetEnrollment(StudentSectionEnrollment v){targetEnrollment=v;} public SemesterRolloverDecision getDecision(){return decision;} public void setDecision(SemesterRolloverDecision v){decision=v;} public String getMessage(){return message;} public void setMessage(String v){message=v;}
}
