package com.collegeerp.erp.timetable.entity;

import com.collegeerp.erp.academic.entity.Section;
import com.collegeerp.erp.academic.entity.SemesterOffering;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.common.entity.BaseAuditEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "weekly_timetables")
public class WeeklyTimetable extends BaseAuditEntity {
    public enum Status { DRAFT, ACTIVE, ARCHIVED }
    public enum ReviewStatus { DRAFT, SUBMITTED, APPROVED, REJECTED, CHANGES_REQUESTED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "college_id", nullable = false) private College college;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false) private Section section;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "semester_offering_id") private SemesterOffering semesterOffering;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(name="review_status",nullable=false,length=30,columnDefinition="varchar(30) default 'DRAFT'") private ReviewStatus reviewStatus=ReviewStatus.DRAFT;
    @Column(name="review_comment",length=1000) private String reviewComment;
    @Column(name="submitted_at") private java.time.LocalDateTime submittedAt;
    @Column(name="reviewed_at") private java.time.LocalDateTime reviewedAt;
    public Long getId(){return id;} public College getCollege(){return college;} public void setCollege(College v){college=v;}
    public Section getSection(){return section;} public void setSection(Section v){section=v;}
    public SemesterOffering getSemesterOffering(){return semesterOffering;} public void setSemesterOffering(SemesterOffering v){semesterOffering=v;}
    public Status getStatus(){return status;} public void setStatus(Status v){status=v;}
    public ReviewStatus getReviewStatus(){return reviewStatus;} public void setReviewStatus(ReviewStatus v){reviewStatus=v;}
    public String getReviewComment(){return reviewComment;} public void setReviewComment(String v){reviewComment=v;}
    public java.time.LocalDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(java.time.LocalDateTime v){submittedAt=v;}
    public java.time.LocalDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(java.time.LocalDateTime v){reviewedAt=v;}
}
