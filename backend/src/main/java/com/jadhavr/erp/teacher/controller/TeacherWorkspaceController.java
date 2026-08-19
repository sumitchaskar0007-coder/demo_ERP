package com.jadhavr.erp.teacher.controller;

import com.jadhavr.erp.common.api.ApiResponse;
import com.jadhavr.erp.teacher.service.*;
import com.jadhavr.erp.teacher.dto.TeacherWorkspaceDtos.StudentIdentifierRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/teacher/workspace")
@PreAuthorize("hasAnyRole('PRINCIPAL','HOD','SUBJECT_TEACHER','CLASS_TEACHER')")
public class TeacherWorkspaceController {
    private final TeacherWorkspaceService workspace;private final TeacherNotificationService notifications;
    public TeacherWorkspaceController(TeacherWorkspaceService w,TeacherNotificationService n){workspace=w;notifications=n;}
    @GetMapping public ApiResponse<?> workspace(@RequestParam(required=false)String search,@RequestParam(required=false)Integer attendanceBelow,@RequestParam(required=false)String gender,@RequestParam(required=false)Long divisionId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.success("Teacher workspace",workspace.workspace(search,attendanceBelow,gender,divisionId,page,size));}
    @GetMapping("/students/{id}/attendance")public ApiResponse<?> attendance(@PathVariable Long id){return ApiResponse.success("Student attendance history",workspace.studentHistory(id));}
    @PutMapping("/students/{id}/identifiers") @PreAuthorize("hasRole('CLASS_TEACHER')") public ApiResponse<?> identifiers(@PathVariable Long id,@Valid @RequestBody StudentIdentifierRequest request){return ApiResponse.success("PRN and roll number saved",workspace.assignIdentifiers(id,request));}
    @PatchMapping("/notifications/{id}/read")public ApiResponse<?> read(@PathVariable Long id){notifications.markRead(id);return ApiResponse.success("Notification marked as read",null);}
    @PatchMapping("/notifications/read-all")public ApiResponse<?> readAll(){notifications.markAllRead();return ApiResponse.success("Notifications marked as read",null);}
}
