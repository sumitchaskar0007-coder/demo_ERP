package com.jadhavr.erp.audit.service;

import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.admission.repository.AdmissionFormRepository;
import com.jadhavr.erp.attendance.entity.WeeklyAttendanceSession;
import com.jadhavr.erp.attendance.repository.WeeklyAttendanceSessionRepository;
import com.jadhavr.erp.audit.dto.BusinessActivityDtos.*;
import com.jadhavr.erp.audit.entity.AuditLog;
import com.jadhavr.erp.audit.enums.*;
import com.jadhavr.erp.audit.repository.AuditLogRepository;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.staff.entity.StaffProfile;
import com.jadhavr.erp.staff.repository.StaffProfileRepository;
import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.repository.UserRepository;
import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository repo;
    private final UserRepository users;
    private final StaffProfileRepository staff;
    private final AdmissionFormRepository admissions;
    private final WeeklyAttendanceSessionRepository attendanceSessions;

    public AuditLogService(AuditLogRepository repo, UserRepository users, StaffProfileRepository staff,
            AdmissionFormRepository admissions, WeeklyAttendanceSessionRepository attendanceSessions) {
        this.repo=repo; this.users=users; this.staff=staff; this.admissions=admissions; this.attendanceSessions=attendanceSessions;
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void log(AuditModule module, AuditAction action, String type, Long id, String description) {
        try { logWithUser(users.findById(SecurityUtils.getCurrentUserId()).orElse(null),module,action,type,id,description); }
        catch(Exception e) { log.warn("Audit logging failed: {}",e.getMessage()); }
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void logWithUser(User user, AuditModule module, AuditAction action, String type, Long id, String description) {
        try { AuditLog row=new AuditLog(); row.setActorUser(user); row.setActorName(user==null?"System":user.getFullName());
            row.setActorEmail(user==null?null:user.getEmail()); row.setActorRoles(user==null?"SYSTEM":user.getRoles().stream()
                    .map(r->r.getName().name()).sorted().collect(Collectors.joining(",")));
            row.setCollege(user==null?null:user.getCollege()); row.setModule(module); row.setAction(action);
            row.setEntityType(type); row.setEntityId(id); row.setDescription(description); repo.save(row);
        } catch(Exception e) { log.warn("Audit logging failed: {}",e.getMessage()); }
    }

    @Transactional(readOnly=true)
    public PageResponse<AuditLog> search(String keyword, Long collegeId, AuditModule module, AuditAction action,
            Long actor, LocalDate from, LocalDate to, int page, int size) {
        return PageResponse.from(repo.findAll(baseSpec(keyword,collegeId,module,action,actor,null,null,from,to),
                PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),100),Sort.by(Sort.Direction.DESC,"createdAt"))));
    }

    @Transactional(readOnly=true)
    public DashboardResponse dashboard(String academicYear, Long departmentId, AuditModule module,
            String role, Long userId, AuditAction action, LocalDate from, LocalDate to,
            String search, int page, int size, String sort) {
        Long collegeId = SecurityUtils.isSuperAdmin() ? null : SecurityUtils.requireCurrentUser().getCollegeId();
        List<StaffProfile> staffRows = collegeId==null ? staff.findAll() : staff.findByCollegeId(collegeId);
        Map<Long,StaffProfile> staffByUser = staffRows.stream().collect(Collectors.toMap(s->s.getUser().getId(), Function.identity(),(a,b)->a));
        Set<Long> departmentActors = departmentId==null ? Set.of() : staffRows.stream()
                .filter(s->s.belongsToDepartment(departmentId)).map(s->s.getUser().getId()).collect(Collectors.toSet());
        Specification<AuditLog> spec=baseSpec(search,collegeId,module,action,userId,role,academicYear,from,to);
        if(departmentId!=null) spec=spec.and((r,q,c)->departmentActors.isEmpty()?c.disjunction():r.get("actorUser").get("id").in(departmentActors));
        spec=applyRoleScope(spec,staffRows);
        Sort ordering="oldest".equals(sort)?Sort.by("createdAt").ascending():Sort.by("createdAt").descending();
        int safeSize=Math.min(Math.max(size,10),100),safePage=Math.max(page,0);
        Page<AuditLog> result=repo.findAll(spec,PageRequest.of(safePage,safeSize,ordering));
        List<AuditLog> all=repo.findAll(spec,Sort.by("createdAt").ascending());
        List<ActivityRow> rows=result.getContent().stream().map(x->businessRow(x,staffByUser)).toList();
        LocalDate today=LocalDate.now(),week=today.minusDays(6),month=today.withDayOfMonth(1);
        long pendingApprovals=pendingApprovals(collegeId,departmentId);
        Summary summary=new Summary(countOn(all,today),countModule(all,AuditModule.ATTENDANCE),
                countModule(all,AuditModule.ADMISSION)+countModule(all,AuditModule.STUDENT_SECTION),countModule(all,AuditModule.FEE),
                all.stream().filter(x->x.getCreatedAt().toLocalDate().equals(today)).filter(this::critical).count(),pendingApprovals);
        List<ActivityRow> timeline=all.stream().filter(x->!x.getCreatedAt().toLocalDate().isBefore(week)).sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed()).limit(12).map(x->businessRow(x,staffByUser)).toList();
        List<ModuleAnalytics> modules=Arrays.stream(AuditModule.values()).filter(m->businessModule(m)).map(m->new ModuleAnalytics(moduleLabel(m),
                all.stream().filter(x->x.getModule()==m&&sameDay(x,today)).count(),
                all.stream().filter(x->x.getModule()==m&&!x.getCreatedAt().toLocalDate().isBefore(week)).count(),
                all.stream().filter(x->x.getModule()==m&&!x.getCreatedAt().toLocalDate().isBefore(month)).count())).filter(x->x.month()>0).toList();
        List<DepartmentAnalytics> departments=departmentAnalytics(all,staffByUser);
        List<TrendPoint> trend=all.stream().collect(Collectors.groupingBy(x->x.getCreatedAt().toLocalDate(),TreeMap::new,Collectors.counting()))
                .entrySet().stream().map(e->new TrendPoint(e.getKey(),e.getValue())).toList();
        List<Distribution> distribution=all.stream().collect(Collectors.groupingBy(x->actionGroup(x.getAction()),LinkedHashMap::new,Collectors.counting()))
                .entrySet().stream().map(e->new Distribution(e.getKey(),e.getValue())).sorted(Comparator.comparing(Distribution::value).reversed()).toList();
        long pendingAttendance=pendingAttendance(collegeId,departmentId,today);
        long studentUpdates=all.stream().filter(x->sameDay(x,today)&&x.getModule()==AuditModule.STUDENT_SECTION&&x.getAction()==AuditAction.UPDATE).count();
        long feeChanges=all.stream().filter(x->x.getModule()==AuditModule.FEE&&x.getEntityType()!=null&&x.getEntityType().toLowerCase().contains("structure")&&x.getAction()==AuditAction.UPDATE).count();
        long timetableChanges=all.stream().filter(x->x.getModule()==AuditModule.ACADEMIC&&x.getDescription().toLowerCase().contains("timetable")).count();
        List<AlertItem> alerts=List.of(new AlertItem("attendance","Attendance Pending",pendingAttendance,"Attendance","SUBMIT"),
                new AlertItem("admissions","Admission Approvals Pending",pendingApprovals,"Admissions","APPROVE"),
                new AlertItem("students","Student Records Updated Today",studentUpdates,"Students","UPDATE"),
                new AlertItem("fees","Fee Structures Modified",feeChanges,"Fees","UPDATE"),
                new AlertItem("timetable","Timetable Changes",timetableChanges,"Academic","UPDATE"));
        List<String> insights=insights(summary,modules,departments,pendingAttendance,pendingApprovals);
        return new DashboardResponse(summary,timeline,modules,departments,trend,distribution,alerts,insights,rows,
                result.getTotalElements(),result.getTotalPages(),result.getNumber(),result.getSize());
    }

    private Specification<AuditLog> baseSpec(String keyword,Long collegeId,AuditModule module,AuditAction action,
            Long actor,String role,String academicYear,LocalDate from,LocalDate to) {
        Long scope=SecurityUtils.isSuperAdmin()?collegeId:SecurityUtils.requireCurrentUser().getCollegeId();
        Specification<AuditLog>s=Specification.where(null);
        if(scope!=null)s=s.and((r,q,c)->c.equal(r.get("college").get("id"),scope));
        if(module!=null)s=s.and((r,q,c)->c.equal(r.get("module"),module)); if(action!=null)s=s.and((r,q,c)->c.equal(r.get("action"),action));
        if(actor!=null)s=s.and((r,q,c)->c.equal(r.get("actorUser").get("id"),actor));
        if(role!=null&&!role.isBlank()){String p="%"+role.toUpperCase()+"%";s=s.and((r,q,c)->c.like(c.upper(r.get("actorRoles")),p));}
        if(academicYear!=null&&!academicYear.isBlank()){String p="%"+academicYear.toLowerCase()+"%";s=s.and((r,q,c)->c.like(c.lower(r.get("description")),p));}
        if(from!=null)s=s.and((r,q,c)->c.greaterThanOrEqualTo(r.get("createdAt"),from.atStartOfDay()));
        if(to!=null)s=s.and((r,q,c)->c.lessThan(r.get("createdAt"),to.plusDays(1).atStartOfDay()));
        if(keyword!=null&&!keyword.isBlank()){String p="%"+keyword.toLowerCase()+"%";s=s.and((r,q,c)->c.or(c.like(c.lower(r.get("description")),p),c.like(c.lower(r.get("actorName")),p),c.like(c.lower(r.get("actorEmail")),p),c.like(c.lower(r.get("entityType")),p)));}
        return s;
    }

    private Specification<AuditLog> applyRoleScope(Specification<AuditLog> spec,List<StaffProfile> staffRows) {
        if(SecurityUtils.isSuperAdmin()||SecurityUtils.isPrincipal())return spec;
        Long current=SecurityUtils.getCurrentUserId();
        if(SecurityUtils.hasRole("HOD")){StaffProfile hod=staffRows.stream().filter(s->s.getUser().getId().equals(current)).findFirst().orElse(null);if(hod==null)return spec.and((r,q,c)->c.disjunction());Set<Long> ids=staffRows.stream().filter(s->s.getDepartments().stream().anyMatch(d->hod.belongsToDepartment(d.getId()))).map(s->s.getUser().getId()).collect(Collectors.toSet());return spec.and((r,q,c)->r.get("actorUser").get("id").in(ids));}
        if(SecurityUtils.hasRole("CLASS_TEACHER")||SecurityUtils.hasRole("SUBJECT_TEACHER"))return spec.and((r,q,c)->c.equal(r.get("actorUser").get("id"),current));
        if(SecurityUtils.hasRole("STUDENT_SECTION")||SecurityUtils.hasRole("FEE_SECTION"))return spec.and((r,q,c)->r.get("module").in(AuditModule.ADMISSION,AuditModule.STUDENT_SECTION,AuditModule.FEE));
        return spec.and((r,q,c)->c.equal(r.get("actorUser").get("id"),current));
    }

    private ActivityRow businessRow(AuditLog x,Map<Long,StaffProfile> staffByUser){StaffProfile s=x.getActorUser()==null?null:staffByUser.get(x.getActorUser().getId());String department=s!=null&&s.getDepartment()!=null?s.getDepartment().getName():defaultDepartment(x);return new ActivityRow(x.getId(),x.getCreatedAt(),x.getActorUser()==null?null:x.getActorUser().getId(),x.getActorName(),primaryRole(x.getActorRoles()),s==null||s.getDepartment()==null?null:s.getDepartment().getId(),department,moduleLabel(x.getModule()),actionLabel(x.getAction()),activityTitle(x),x.getDescription(),activityStatus(x),x.getEntityType()==null?"—":x.getEntityType()+(x.getEntityId()==null?"":" #"+x.getEntityId()),x.getDescription(),null,null);}
    private List<DepartmentAnalytics> departmentAnalytics(List<AuditLog> all,Map<Long,StaffProfile> staffByUser){Map<String,List<AuditLog>> groups=all.stream().collect(Collectors.groupingBy(x->{StaffProfile s=x.getActorUser()==null?null:staffByUser.get(x.getActorUser().getId());return s!=null&&s.getDepartment()!=null?s.getDepartment().getName():defaultDepartment(x);},LinkedHashMap::new,Collectors.toList()));return groups.entrySet().stream().map(e->{List<AuditLog> g=e.getValue();Map<String,Long> actors=g.stream().collect(Collectors.groupingBy(AuditLog::getActorName,Collectors.counting()));String active=actors.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("System");StaffProfile profile=g.stream().map(x->x.getActorUser()==null?null:staffByUser.get(x.getActorUser().getId())).filter(Objects::nonNull).filter(x->x.getDepartment()!=null).findFirst().orElse(null);return new DepartmentAnalytics(profile==null?null:profile.getDepartment().getId(),e.getKey(),g.size(),g.stream().map(AuditLog::getCreatedAt).max(LocalDateTime::compareTo).orElse(null),active,g.size()>=20?"HIGH":g.size()>=5?"ACTIVE":"NORMAL");}).sorted(Comparator.comparing(DepartmentAnalytics::activities).reversed()).toList();}
    private List<String> insights(Summary summary,List<ModuleAnalytics> modules,List<DepartmentAnalytics> departments,long pendingAttendance,long pendingApprovals){List<String> result=new ArrayList<>();modules.stream().max(Comparator.comparing(ModuleAnalytics::today)).ifPresent(x->result.add(x.module()+" had the highest module activity today with "+x.today()+" activities."));departments.stream().findFirst().ifPresent(x->result.add(x.department()+" was the most active department with "+x.activities()+" activities."));result.add(pendingAttendance+" attendance entries are still pending.");result.add(pendingApprovals+" admissions are awaiting approval.");result.add(summary.criticalChanges()+" important record changes were made today.");return result;}
    private long pendingApprovals(Long collegeId,Long departmentId){List<AdmissionForm> rows=collegeId==null?admissions.findAll():admissions.findByCollegeId(collegeId);return rows.stream().filter(a->departmentId==null||a.getDepartment().getId().equals(departmentId)).filter(a->a.getStatus()==AdmissionStatus.PRINCIPAL_REVIEW_PENDING||a.getStatus()==AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING).count();}
    private long pendingAttendance(Long collegeId,Long departmentId,LocalDate day){List<WeeklyAttendanceSession> rows=collegeId==null?attendanceSessions.findAll():attendanceSessions.findByCollegeIdAndAttendanceDateBetweenOrderByAttendanceDateDescStartTimeDesc(collegeId,day,day);return rows.stream().filter(x->departmentId==null||x.getSection().getDepartment().getId().equals(departmentId)).filter(x->x.getStatus()==WeeklyAttendanceSession.Status.DRAFT).count();}
    private long countOn(List<AuditLog> rows,LocalDate day){return rows.stream().filter(x->sameDay(x,day)).count();}private long countModule(List<AuditLog> rows,AuditModule module){return rows.stream().filter(x->x.getModule()==module).count();}private boolean sameDay(AuditLog x,LocalDate d){return x.getCreatedAt().toLocalDate().equals(d);}private boolean critical(AuditLog x){return Set.of(AuditAction.UPDATE,AuditAction.REJECT,AuditAction.DEACTIVATE).contains(x.getAction());}
    private boolean businessModule(AuditModule m){return !Set.of(AuditModule.AUTH,AuditModule.ACCOUNT,AuditModule.SYSTEM).contains(m);}private String moduleLabel(AuditModule m){return switch(m){case ADMISSION,STUDENT_SECTION->"Admissions";case FEE->"Fees";case ACADEMIC->"Academic";case ATTENDANCE->"Attendance";case STAFF,USER->"Staff";case REPORT->"Reports";case DEPARTMENT->"Departments";case COLLEGE->"College";default->title(m.name());};}private String defaultDepartment(AuditLog x){return x.getModule()==AuditModule.FEE?"Finance":x.getModule()==AuditModule.ADMISSION||x.getModule()==AuditModule.STUDENT_SECTION?"Admissions":"Administration";}private String primaryRole(String roles){if(roles==null||roles.isBlank())return"System";return title(roles.split(",")[0]);}private String actionLabel(AuditAction a){return title(a.name());}private String actionGroup(AuditAction a){return switch(a){case CREATE,SUBMIT->"Create";case UPDATE,ACTIVATE,DEACTIVATE,ASSIGN,MARK_ATTENDANCE->"Update";case APPROVE,VERIFY->"Approve";case REJECT->"Reject";case VIEW_REPORT,PRINT,EXPORT->"View / Export";default->title(a.name());};}private String activityStatus(AuditLog x){return x.getAction()==AuditAction.REJECT||x.getAction()==AuditAction.DEACTIVATE?"CANCELLED":x.getAction()==AuditAction.SUBMIT?"PENDING":x.getAction()==AuditAction.UPDATE?"WARNING":"SUCCESS";}private String activityTitle(AuditLog x){return actionLabel(x.getAction())+" "+(x.getEntityType()==null?moduleLabel(x.getModule()):title(x.getEntityType()));}private String title(String s){return Arrays.stream(s.toLowerCase().split("[_ ]")).filter(x->!x.isBlank()).map(x->Character.toUpperCase(x.charAt(0))+x.substring(1)).collect(Collectors.joining(" "));}
}
