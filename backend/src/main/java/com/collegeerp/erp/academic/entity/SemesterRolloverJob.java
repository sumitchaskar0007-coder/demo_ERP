package com.collegeerp.erp.academic.entity;

import com.collegeerp.erp.academic.entity.AcademicModels.AcademicTerm;
import com.collegeerp.erp.academic.enums.SemesterRolloverStatus;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="semester_rollover_jobs",uniqueConstraints=@UniqueConstraint(columnNames={"college_id","source_term_id","target_term_id"}))
public class SemesterRolloverJob extends BaseAuditEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="college_id") private College college;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="source_term_id") private AcademicTerm sourceTerm;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="target_term_id") private AcademicTerm targetTerm;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SemesterRolloverStatus status;
    private int totalStudents; private int promotedStudents; private int heldStudents; private int graduatedStudents;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="requested_by") private User requestedBy;
    private LocalDateTime startedAt; private LocalDateTime completedAt; @Column(length=500) private String failureReason;
    public Long getId(){return id;} public College getCollege(){return college;} public void setCollege(College v){college=v;}
    public AcademicTerm getSourceTerm(){return sourceTerm;} public void setSourceTerm(AcademicTerm v){sourceTerm=v;} public AcademicTerm getTargetTerm(){return targetTerm;} public void setTargetTerm(AcademicTerm v){targetTerm=v;}
    public SemesterRolloverStatus getStatus(){return status;} public void setStatus(SemesterRolloverStatus v){status=v;} public int getTotalStudents(){return totalStudents;} public void setTotalStudents(int v){totalStudents=v;} public int getPromotedStudents(){return promotedStudents;} public void setPromotedStudents(int v){promotedStudents=v;} public int getHeldStudents(){return heldStudents;} public void setHeldStudents(int v){heldStudents=v;} public int getGraduatedStudents(){return graduatedStudents;} public void setGraduatedStudents(int v){graduatedStudents=v;} public User getRequestedBy(){return requestedBy;} public void setRequestedBy(User v){requestedBy=v;} public LocalDateTime getStartedAt(){return startedAt;} public void setStartedAt(LocalDateTime v){startedAt=v;} public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime v){completedAt=v;} public String getFailureReason(){return failureReason;} public void setFailureReason(String v){failureReason=v;}
}
