package com.collegeerp.erp.teacher.entity;

import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.staff.entity.StaffProfile;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="teacher_notifications",indexes={@Index(name="idx_teacher_notification_inbox",columnList="teacher_id,created_at"),@Index(name="idx_teacher_notification_unread",columnList="teacher_id,read_at")})
public class TeacherNotification extends BaseAuditEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="teacher_id",nullable=false) private StaffProfile teacher;
    @Column(nullable=false,length=40) private String type;
    @Column(nullable=false,length=500) private String message;
    @Column(name="read_at") private LocalDateTime readAt;
    public Long getId(){return id;} public StaffProfile getTeacher(){return teacher;} public void setTeacher(StaffProfile v){teacher=v;}
    public String getType(){return type;} public void setType(String v){type=v;} public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public LocalDateTime getReadAt(){return readAt;} public void setReadAt(LocalDateTime v){readAt=v;}
}
