package com.jadhavr.erp.timetable.entity;

import com.jadhavr.erp.academic.entity.Section;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.common.entity.BaseAuditEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "weekly_timetables", uniqueConstraints =
        @UniqueConstraint(name = "uk_weekly_timetable_section", columnNames = "section_id"))
public class WeeklyTimetable extends BaseAuditEntity {
    public enum Status { DRAFT, ACTIVE, ARCHIVED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "college_id", nullable = false) private College college;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false) private Section section;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.ACTIVE;
    public Long getId(){return id;} public College getCollege(){return college;} public void setCollege(College v){college=v;}
    public Section getSection(){return section;} public void setSection(Section v){section=v;}
    public Status getStatus(){return status;} public void setStatus(Status v){status=v;}
}
