package com.jadhavr.erp.bootstrap;

import com.jadhavr.erp.user.entity.RoleName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/** One-time, explicitly enabled reset that creates a comprehensive local review dataset. */
@Component
@Order(100)
@ConditionalOnProperty(name = "app.demo-data.reset", havingValue = "true")
public class DemoReviewDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoReviewDataSeeder.class);
    private static final String CONFIRMATION = "RESET_JADHAVR_ERP";
    private static final String YEAR = "2026-27";
    private static final String DEMO_PASSWORD = "Demo@12345";
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final String confirmation;

    public DemoReviewDataSeeder(JdbcTemplate jdbc, PasswordEncoder passwords,
            @org.springframework.beans.factory.annotation.Value("${app.demo-data.confirm:}") String confirmation) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.confirmation = confirmation;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!CONFIRMATION.equals(confirmation)) throw new IllegalStateException("Demo reset confirmation is missing");
        log.warn("Resetting all ERP business data and creating the comprehensive review dataset");
        truncatePublicTables();
        Map<String, Long> roles = seedRoles();
        String adminHash = passwords.encode("Admin@12345");
        String demoHash = passwords.encode(DEMO_PASSWORD);
        long admin = user(null, "Super Admin", "admin@erp.com", "9999999999", adminHash, roles.get("SUPER_ADMIN"));
        long college = insert("insert into colleges(name,code,address,city,state,pincode,contact_email,contact_phone,status,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,now(),now()) returning id",
                "Jadhavr Institute of Higher Education", "JIHE", "Knowledge Park, University Road", "Pune", "Maharashtra", "411038", "office@jihe.edu", "02025550001", "ACTIVE");
        long cs = department(college, "Computer Science", "CS", "BCA, BSc CS and software technology programmes");
        long commerce = department(college, "Commerce and Management", "COM", "BCom, accounting and management programmes");

        long principal = user(college, "Dr. Ananya Deshmukh", "principal@jihe.edu", "9000000001", demoHash, roles.get("PRINCIPAL"));
        long studentOfficer = staffUser(college, cs, "Meera Patil", "studentsection@jihe.edu", "9000000002", "STU-001", "STUDENT_SECTION", demoHash, roles, "STUDENT_SECTION");
        long feeOfficer = staffUser(college, commerce, "Rohan Kulkarni", "fees@jihe.edu", "9000000003", "FEE-001", "FEE_SECTION", demoHash, roles, "FEE_SECTION");
        long hodCs = staffUser(college, cs, "Prof. Vikram Joshi", "hod.cs@jihe.edu", "9000000004", "HOD-CS-01", "HOD", demoHash, roles, "HOD");
        long hodCommerce = staffUser(college, commerce, "Prof. Neha Shah", "hod.commerce@jihe.edu", "9000000005", "HOD-COM-01", "HOD", demoHash, roles, "HOD");
        long teacherCsA = staffUser(college, cs, "Prof. Aarav Jadhav", "teacher.csa@jihe.edu", "9000000011", "T-CS-001", "CLASS_TEACHER", demoHash, roles, "CLASS_TEACHER", "SUBJECT_TEACHER");
        long teacherCsB = staffUser(college, cs, "Prof. Isha More", "teacher.csb@jihe.edu", "9000000012", "T-CS-002", "CLASS_TEACHER", demoHash, roles, "CLASS_TEACHER", "SUBJECT_TEACHER");
        long teacherComA = staffUser(college, commerce, "Prof. Kabir Mehta", "teacher.coma@jihe.edu", "9000000013", "T-COM-001", "CLASS_TEACHER", demoHash, roles, "CLASS_TEACHER", "SUBJECT_TEACHER");
        long teacherComB = staffUser(college, commerce, "Prof. Sana Khan", "teacher.comb@jihe.edu", "9000000014", "T-COM-002", "CLASS_TEACHER", demoHash, roles, "CLASS_TEACHER", "SUBJECT_TEACHER");
        long subjectTeacher = staffUser(college, cs, "Prof. Reyansh Rao", "subject.teacher@jihe.edu", "9000000015", "T-MULTI-01", "SUBJECT_TEACHER", demoHash, roles, "SUBJECT_TEACHER");
        long generalStaff = staffUser(college, commerce, "Kavya Nair", "staff@jihe.edu", "9000000016", "GEN-001", "GENERAL_STAFF", demoHash, roles, "GENERAL_STAFF");
        addStaffDepartment(teacherCsA, commerce); // multi-department + multi-role review account
        addStaffDepartment(subjectTeacher, commerce);

        Map<Long, List<Long>> classes = new LinkedHashMap<>();
        Map<Long, List<Long>> sections = new LinkedHashMap<>();
        Map<Long, List<Long>> subjects = new LinkedHashMap<>();
        seedAcademicStructure(college, cs, "BSc Computer Science", "BSC-CS", classes, sections, subjects);
        seedAcademicStructure(college, commerce, "Bachelor of Commerce", "BCOM", classes, sections, subjects);

        long csFy = classes.get(cs).get(0), comFy = classes.get(commerce).get(0);
        List<Long> csSections = sections.get(csFy), comSections = sections.get(comFy);
        assignClassTeacher(csSections.get(0), teacherCsA); assignClassTeacher(csSections.get(1), teacherCsB);
        assignClassTeacher(comSections.get(0), teacherComA); assignClassTeacher(comSections.get(1), teacherComB);
        assignSubjects(subjects.get(csFy), teacherCsA, teacherCsB, subjectTeacher);
        assignSubjects(subjects.get(comFy), teacherComA, teacherComB, teacherCsA, subjectTeacher);

        List<TimetableSeed> timetables = List.of(
                timetable(college, csSections.get(0), teacherCsA, subjects.get(csFy), true),
                timetable(college, csSections.get(1), teacherCsB, subjects.get(csFy), false),
                timetable(college, comSections.get(0), teacherComA, subjects.get(comFy), false),
                timetable(college, comSections.get(1), teacherComB, subjects.get(comFy), false));

        long feeCs = feeStructure(college, cs, "Computer Science FY Fees", 65000, 15000);
        long feeCom = feeStructure(college, commerce, "Commerce FY Fees", 48000, 12000);
        List<StudentSeed> studentSeeds = seedStudents(college, cs, commerce, csSections, comSections, feeCs, feeCom,
                principal, studentOfficer, feeOfficer, roles.get("STUDENT"), demoHash);
        seedAttendance(college, timetables, studentSeeds);
        seedNotices(college, cs, commerce, admin, principal, hodCs, hodCommerce);
        seedAudit(college, admin, studentSeeds.size());
        log.warn("Review dataset ready: college={}, departments=2, students={}, demo password={}", college, studentSeeds.size(), DEMO_PASSWORD);
    }

    private void truncatePublicTables() {
        List<String> tables = jdbc.queryForList("select table_name from information_schema.tables where table_schema='public' and table_type='BASE TABLE' and table_name<>'flyway_schema_history'", String.class);
        if (tables.isEmpty()) return;
        String names = tables.stream().map(name -> "\"" + name.replace("\"", "\"\"") + "\"").collect(java.util.stream.Collectors.joining(","));
        jdbc.execute("TRUNCATE TABLE " + names + " RESTART IDENTITY CASCADE");
    }

    private Map<String, Long> seedRoles() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (RoleName role : RoleName.values()) result.put(role.name(), insert("insert into roles(name,description,created_at,updated_at) values(?,?,now(),now()) returning id", role.name(), role.name().replace('_',' ') + " role"));
        return result;
    }

    private long user(Long college, String name, String email, String phone, String hash, Long... roleIds) {
        long id = insert("insert into users(college_id,full_name,email,phone,password_hash,must_change_password,email_verified,status,failed_login_attempts,session_version,created_at,updated_at) values(?,?,?,?,?,false,true,'ACTIVE',0,0,now(),now()) returning id",
                college, name, email, phone, hash);
        for (Long role : roleIds) jdbc.update("insert into user_roles(user_id,role_id) values(?,?)", id, role);
        return id;
    }

    private long staffUser(long college, long department, String name, String email, String phone, String code,
            String type, String hash, Map<String, Long> roles, String... roleNames) {
        Long[] ids = Arrays.stream(roleNames).map(roles::get).toArray(Long[]::new);
        long user = user(college, name, email, phone, hash, ids);
        long profile = insert("insert into staff_profiles(user_id,college_id,department_id,employee_code,full_name,email,phone,staff_type,status,joining_date,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,'2025-06-01',now(),now()) returning id",
                user, college, department, code, name, email, phone, type, "ACTIVE");
        addStaffDepartment(profile, department);
        return profile;
    }
    private void addStaffDepartment(long profile, long department) { jdbc.update("insert into staff_profile_departments(staff_profile_id,department_id) values(?,?) on conflict do nothing", profile, department); }
    private long department(long college, String name, String code, String description) { return insert("insert into departments(college_id,name,code,description,status,created_at,updated_at) values(?,?,?,?, 'ACTIVE',now(),now()) returning id", college,name,code,description); }

    private void seedAcademicStructure(long college, long department, String programme, String prefix,
            Map<Long,List<Long>> classes, Map<Long,List<Long>> sections, Map<Long,List<Long>> subjects) {
        String[] names = {"First Year " + programme, "Second Year " + programme, "Third Year " + programme};
        String[] codes = {prefix+"-FY", prefix+"-SY", prefix+"-TY"};
        String[] years = {"FIRST_YEAR","SECOND_YEAR","THIRD_YEAR"};
        List<Long> departmentClasses = new ArrayList<>();
        for (int index=0; index<3; index++) {
            long academicClass = insert("insert into course_years(college_id,department_id,academic_year,year_name,name,code,description,status,created_at,updated_at) values(?,?,?,?,?,?,?,'ACTIVE',now(),now()) returning id",
                    college,department,YEAR,years[index],names[index],codes[index],"Academic year review data");
            departmentClasses.add(academicClass);
            List<Long> classSections = new ArrayList<>();
            for (String division : List.of("A","B")) classSections.add(insert("insert into course_year_divisions(college_id,department_id,academic_class_id,academic_year,name,code,capacity,status,created_at,updated_at) values(?,?,?,?,?,?,60,'ACTIVE',now(),now()) returning id",
                    college,department,academicClass,YEAR,"Division " + division,division));
            sections.put(academicClass,classSections);
            List<Long> classSubjects = new ArrayList<>();
            String[] subjectNames = prefix.startsWith("BSC")
                    ? new String[]{"Programming Fundamentals","Database Management","Computer Networks"}
                    : new String[]{"Financial Accounting","Business Economics","Business Communication"};
            for(int s=0;s<subjectNames.length;s++) classSubjects.add(insert("insert into course_year_subjects(college_id,department_id,academic_class_id,academic_year,name,code,description,credits,status,subject_type,created_at,updated_at) values(?,?,?,?,?,?,?,4,'ACTIVE',?,now(),now()) returning id",
                    college,department,academicClass,YEAR,subjectNames[s]+" " +(index+1),prefix+"-"+(index+1)+"0"+(s+1),"Production review subject",s==2?"PRACTICAL":"THEORY"));
            subjects.put(academicClass,classSubjects);
        }
        classes.put(department,departmentClasses);
    }

    private void assignClassTeacher(long section, long teacher) { jdbc.update("update course_year_divisions set class_teacher_id=?,updated_at=now() where id=?",teacher,section); }
    private void assignSubjects(List<Long> subjects, long... teachers) {
        for(int i=0;i<subjects.size();i++) for(long teacher:teachers) jdbc.update("insert into subject_teacher_assignments(subject_id,teacher_id,academic_year,status,assigned_at,created_at,updated_at) values(?, ?,?,'ACTIVE',now(),now(),now())",subjects.get(i),teacher,YEAR);
    }

    private TimetableSeed timetable(long college, long section, long teacher, List<Long> subjects, boolean liveReview) {
        long table = insert("insert into weekly_timetables(college_id,section_id,status,created_at,updated_at) values(?,?,'ACTIVE',now(),now()) returning id",college,section);
        String[] labels={"Period 1","Period 2","Short Break","Period 3","Period 4","Lunch Break","Period 5","Period 6","Period 7"};
        String[] starts={"08:30","09:20","10:10","10:25","11:15","12:05","12:45","13:35","14:25"};
        String[] ends={"09:20","10:10","10:25","11:15","12:05","12:45","13:35","14:25","15:15"};
        String[] kinds={"TEACHING","TEACHING","SHORT_BREAK","TEACHING","TEACHING","LUNCH_BREAK","TEACHING","TEACHING","TEACHING"};
        List<Long> periods=new ArrayList<>();
        for(int i=0;i<labels.length;i++) {
            String start=starts[i],end=ends[i];
            if(liveReview&&i==8){ LocalTime now=LocalTime.now(); start=now.minusMinutes(10).withSecond(0).withNano(0).toString(); end=now.plusMinutes(90).withSecond(0).withNano(0).toString(); }
            periods.add(insert("insert into weekly_timetable_periods(timetable_id,position,label,start_time,end_time,kind,created_at,updated_at) values(?,?,?,?,?,?,now(),now()) returning id",table,i+1,labels[i],LocalTime.parse(start),LocalTime.parse(end),kinds[i]));
        }
        List<Long> mondayEntries=new ArrayList<>();
        for(DayOfWeek day:List.of(DayOfWeek.MONDAY,DayOfWeek.TUESDAY,DayOfWeek.WEDNESDAY,DayOfWeek.THURSDAY,DayOfWeek.FRIDAY,DayOfWeek.SATURDAY)) {
            int subjectIndex=0;
            for(int i=0;i<periods.size();i++) if("TEACHING".equals(kinds[i])) {
                long subject=subjects.get(subjectIndex++%subjects.size());
                long entry=insert("insert into weekly_timetable_entries(timetable_id,day_of_week,period_id,subject_id,teacher_id,room,lecture_type,remarks,created_at,updated_at) values(?,?,?,?,?,?,?,?,now(),now()) returning id",
                        table,day.name(),periods.get(i),subject,teacher,"R-"+(100+section),subjectIndex%3==0?"PRACTICAL":"THEORY","Seeded review lecture");
                if(day==DayOfWeek.MONDAY)mondayEntries.add(entry);
            }
        }
        return new TimetableSeed(table,section,teacher,mondayEntries.get(0));
    }

    private long feeStructure(long college,long department,String title,int total,int minimum){return insert("insert into fee_structures(college_id,department_id,academic_year,student_category,title,description,total_fee,minimum_amount_for_admission,admission_fee,tuition_fee,exam_fee,library_fee,other_fee,status,created_at,updated_at) values(?,?,?,'OPEN',?,?,?, ?,5000,?,3000,2000,5000,'ACTIVE',now(),now()) returning id",college,department,YEAR,title,"Complete review fee structure",total,minimum,total-15000);}

    private List<StudentSeed> seedStudents(long college,long cs,long commerce,List<Long> csSections,List<Long> comSections,
            long feeCs,long feeCom,long principal,long studentOfficer,long feeOfficer,long studentRole,String hash){
        List<StudentSeed> result=new ArrayList<>();
        String[] first={"Aarav","Aditi","Arjun","Anaya","Vivaan","Diya","Kabir","Ira","Reyansh","Myra","Atharv","Saanvi","Advik","Kiara","Rudra","Riya","Vihaan","Anvi","Dhruv","Sara"};
        String[] last={"Patil","Shinde","Kulkarni","Jadhav","Joshi","More","Pawar","Desai","Mehta","Khan"};
        for(int i=1;i<=100;i++){
            boolean isCs=i<=50; long dept=isCs?cs:commerce; int local=isCs?i:i-50; long section=(isCs?csSections:comSections).get((local-1)/25); long fee=isCs?feeCs:feeCom;
            String fn=first[(i-1)%first.length],ln=last[(i-1)%last.length],name=fn+" "+ln,email=String.format("student%03d@jihe.edu",i),phone=String.format("91%08d",i);
            long user=user(college,name,email,phone,hash,studentRole);
            long student=insert("insert into student_profiles(user_id,college_id,department_id,admission_number,student_category,first_name,last_name,full_name,email,phone,date_of_birth,gender,address_line1,city,state,pincode,parent_name,parent_phone,parent_email,status,roll_number,activated_at,created_at,updated_at) values(?,?,?,?,'OPEN',?,?,?,?,? ,?,?,?,'Pune','Maharashtra','411038',?,?,?,'ACTIVE',?,now(),now(),now()) returning id",
                    user,college,dept,String.format("JIHE26%04d",i),fn,ln,name,email,phone,LocalDate.of(2007+(i%2),(i%12)+1,(i%27)+1),i%2==0?"Female":"Male","Student Address "+i,"Parent of "+name,String.format("98%08d",i),"parent"+i+"@example.com",String.format("%s-%03d",isCs?"CS":"COM",local));
            jdbc.update("insert into student_section_enrollments(student_id,section_id,academic_class_id,academic_year,roll_number,status,enrolled_at,created_at,updated_at) select ?,s.id,s.academic_class_id,?,?,'ACTIVE',now(),now(),now() from course_year_divisions s where s.id=?",student,YEAR,String.format("%03d",local),section);
            long admission=insert("insert into admission_forms(admission_reference_number,college_id,department_id,student_id,student_user_id,academic_year,student_category,first_name,last_name,full_name,email,phone,date_of_birth,gender,address_line1,city,state,pincode,parent_name,parent_phone,parent_email,previous_school_name,previous_class_name,previous_percentage,status,source,submitted_at,student_section_verified_at,student_section_verified_by,student_section_remarks,principal_approved_at,details_completed_at,print_count,created_at,updated_at) values(?,?,?,?,?,?,'OPEN',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PRINCIPAL_APPROVED','PUBLIC_LINK',now()-interval '30 days',now()-interval '25 days',?,'Documents verified',now()-interval '20 days',now()-interval '30 days',1,now(),now()) returning id",
                    String.format("ADM-2026-%04d",i),college,dept,student,user,YEAR,fn,ln,name,email,phone,LocalDate.of(2007+(i%2),(i%12)+1,(i%27)+1),i%2==0?"Female":"Male","Student Address "+i,"Pune","Maharashtra","411038","Parent of "+name,String.format("98%08d",i),"parent"+i+"@example.com","Jadhavr Higher Secondary School","12th",70+(i%25),studentOfficer);
            jdbc.update("insert into admission_status_history(admission_form_id,changed_by,old_status,new_status,action,remarks,created_at,updated_at) values(?,?,'PRINCIPAL_REVIEW_PENDING','PRINCIPAL_APPROVED','PRINCIPAL_APPROVED','Approved for review dataset',now(),now())",admission,principal);
            int total=isCs?65000:48000; int paid=i%4==0?total:i%3==0?total/2:0; String accountStatus=paid==total?"PAID":paid>0?"PARTIALLY_PAID":"PENDING";
            long account=insert("insert into student_fee_accounts(student_id,student_user_id,admission_form_id,college_id,department_id,fee_structure_id,academic_year,student_category,total_fee,paid_amount,remaining_amount,discount_amount,minimum_amount_for_admission,status,created_at,updated_at) values(?,?,?,?,?,?,?,'OPEN',?,?,?,?,?,?,now(),now()) returning id",student,user,admission,college,dept,fee,YEAR,total,paid,total-paid,0,isCs?15000:12000,accountStatus);
            if(i<=24){String paymentStatus=i<=8?"VERIFIED":i<=16?"PENDING":"REJECTED"; int amount=isCs?15000:12000; long payment=insert("insert into fee_payments(fee_account_id,student_id,student_user_id,college_id,department_id,amount,payment_mode,transaction_reference,payment_date,proof_url,remarks,status,submitted_at,verified_at,verified_by_id,rejected_at,rejected_by_id,rejection_reason,created_at,updated_at) values(?,?,?,?,?,?,?, ?,current_date-?,?,'Seeded payment',?,now(),?,?,?, ?,?,now(),now()) returning id",account,student,user,college,dept,amount,i%2==0?"UPI":"BANK_TRANSFER",String.format("TXN-DEMO-%04d",i),i%20,"/demo/payment-proof-"+i+".png",paymentStatus,"VERIFIED".equals(paymentStatus)?LocalDateTime.now():null,"VERIFIED".equals(paymentStatus)?feeOfficer:null,"REJECTED".equals(paymentStatus)?LocalDateTime.now():null,"REJECTED".equals(paymentStatus)?feeOfficer:null,"REJECTED".equals(paymentStatus)?"Example unclear payment proof":null);
                if("VERIFIED".equals(paymentStatus)) jdbc.update("insert into fee_transactions(fee_account_id,fee_payment_id,transaction_type,amount,previous_paid_amount,new_paid_amount,previous_remaining_amount,new_remaining_amount,remarks,performed_by_id,created_at,updated_at) values(?,?,'PAYMENT_VERIFIED',?,0,?,?,?,'Verified demo payment',?,now(),now())",account,payment,amount,amount,total,total-amount,feeOfficer);
            }
            result.add(new StudentSeed(student,section));
        }
        return result;
    }

    private void seedAttendance(long college,List<TimetableSeed> timetables,List<StudentSeed> students){
        for(TimetableSeed table:timetables){ List<Long> roster=students.stream().filter(s->s.section==table.section).map(s->s.student).toList();
            for(int week=1;week<=8;week++){LocalDate date=LocalDate.now().minusWeeks(week).with(DayOfWeek.MONDAY);long session=insert("insert into weekly_attendance_sessions(college_id,timetable_entry_id,section_id,subject_id,teacher_id,attendance_date,start_time,end_time,lecture_number,status,submitted_at,created_at,updated_at) select ?,e.id,wt.section_id,e.subject_id,e.teacher_id,?,'08:30','09:20',1,'SUBMITTED',now(),now(),now() from weekly_timetable_entries e join weekly_timetables wt on wt.id=e.timetable_id where e.id=? returning id",college,date,table.mondayEntry);
                for(int i=0;i<roster.size();i++){String status=(i+week)%13==0?"ABSENT":(i+week)%9==0?"LATE":(i+week)%17==0?"LEAVE":"PRESENT";jdbc.update("insert into weekly_attendance_records(session_id,student_id,status,remarks,created_at,updated_at) values(?,?,?, ?,now(),now())",session,roster.get(i),status,"ABSENT".equals(status)?"Absent in seeded history":null);}
            }
        }
    }

    private void seedNotices(long college,long cs,long commerce,long admin,long principal,long hodCs,long hodCommerce){
        notice(college,null,admin,"Welcome to Jadhavr ERP","This workspace contains a complete review dataset for every major module.",List.of("PRINCIPAL","HOD","CLASS_TEACHER","SUBJECT_TEACHER","STUDENT"));
        notice(college,null,principal,"Academic Year 2026-27","The academic timetable and student allocations are now active.",List.of("HOD","CLASS_TEACHER","SUBJECT_TEACHER","STUDENT"));
        notice(college,cs,hodCs,"Computer Science Lab Schedule","Students should check the weekly timetable for practical sessions.",List.of("CLASS_TEACHER","SUBJECT_TEACHER","STUDENT"));
        notice(college,commerce,hodCommerce,"Commerce Seminar","Department seminar review notice for all commerce students.",List.of("CLASS_TEACHER","SUBJECT_TEACHER","STUDENT"));
    }
    private void notice(long college,Long department,long author,String title,String message,List<String> audiences){long id=insert("insert into notices(title,message,created_by_user_id,department_id,created_at,updated_at) values(?,?,?,?,now(),now()) returning id",title,message,author,department);jdbc.update("insert into notice_colleges(notice_id,college_id) values(?,?)",id,college);for(String role:audiences)jdbc.update("insert into notice_audience_roles(notice_id,role_name) values(?,?)",id,role);}
    private void seedAudit(long college,long admin,int students){jdbc.update("insert into erp_audit_logs(college_id,action,entity_type,entity_id,actor_id,details,created_at,updated_at) values(?,'DEMO_REVIEW_DATA_CREATED','College',?,? ,?,now(),now())",college,college,admin,"Created comprehensive dataset with "+students+" approved students");}
    private long insert(String sql,Object...args){Long id=jdbc.queryForObject(sql,Long.class,args);if(id==null)throw new IllegalStateException("Insert did not return an id");return id;}
    private record TimetableSeed(long timetable,long section,long teacher,long mondayEntry){}
    private record StudentSeed(long student,long section){}
}
