package com.collegeerp.erp.teacher.repository;
import com.collegeerp.erp.teacher.entity.TeacherNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TeacherNotificationRepository extends JpaRepository<TeacherNotification,Long>{List<TeacherNotification> findByTeacherIdOrderByCreatedAtDesc(Long teacherId,Pageable pageable);long countByTeacherIdAndReadAtIsNull(Long teacherId);}
