package com.collegeerp.erp.teacher.service;

import com.collegeerp.erp.academic.entity.Section;
import com.collegeerp.erp.academic.enums.AcademicStatus;
import com.collegeerp.erp.academic.repository.SubjectTeacherAssignmentRepository;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.staff.entity.StaffProfile;
import com.collegeerp.erp.staff.repository.StaffProfileRepository;
import com.collegeerp.erp.teacher.entity.TeacherNotification;
import com.collegeerp.erp.teacher.repository.TeacherNotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @Transactional
public class TeacherNotificationService {
    private final TeacherNotificationRepository notifications;private final SubjectTeacherAssignmentRepository assignments;private final StaffProfileRepository staff;
    public TeacherNotificationService(TeacherNotificationRepository n,SubjectTeacherAssignmentRepository a,StaffProfileRepository s){notifications=n;assignments=a;staff=s;}
    public void notifyTeacher(StaffProfile teacher,String type,String message){if(teacher==null)return;TeacherNotification row=new TeacherNotification();row.setTeacher(teacher);row.setType(type);row.setMessage(message);notifications.save(row);}
    public void notifyDivision(Section section,String type,String message){Set<Long> sent=new HashSet<>();if(section.getClassTeacher()!=null&&sent.add(section.getClassTeacher().getId()))notifyTeacher(section.getClassTeacher(),type,message);assignments.findBySubjectDepartmentIdAndStatus(section.getDepartment().getId(),AcademicStatus.ACTIVE).stream().filter(a->a.getSections().stream().anyMatch(s->s.getId().equals(section.getId()))).map(a->a.getTeacher()).filter(t->sent.add(t.getId())).forEach(t->notifyTeacher(t,type,message));}
    @Transactional(readOnly=true) public List<TeacherNotification> inbox(Long teacherId){return notifications.findByTeacherIdOrderByCreatedAtDesc(teacherId,PageRequest.of(0,100));}
    public void markRead(Long id){StaffProfile current=staff.findByUserId(SecurityUtils.getCurrentUserId()).orElseThrow(()->new AccessDeniedException("Teacher profile not found"));TeacherNotification n=notifications.findById(id).orElseThrow(()->new ResourceNotFoundException("Notification not found"));if(!n.getTeacher().getId().equals(current.getId()))throw new AccessDeniedException("Notification is outside your scope");n.setReadAt(LocalDateTime.now());notifications.save(n);}
    public void markAllRead(){StaffProfile current=staff.findByUserId(SecurityUtils.getCurrentUserId()).orElseThrow(()->new AccessDeniedException("Teacher profile not found"));notifications.findByTeacherIdOrderByCreatedAtDesc(current.getId(),PageRequest.of(0,500)).stream().filter(n->n.getReadAt()==null).forEach(n->n.setReadAt(LocalDateTime.now()));}
}
