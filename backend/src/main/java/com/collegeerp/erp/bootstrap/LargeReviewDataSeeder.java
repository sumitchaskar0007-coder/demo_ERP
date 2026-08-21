package com.collegeerp.erp.bootstrap;

import com.collegeerp.erp.user.entity.RoleName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Explicit, one-time PostgreSQL load dataset for local performance and workflow review. */
@Component
@Order(100)
@ConditionalOnProperty(name = "app.large-demo.reset", havingValue = "true")
public class LargeReviewDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(LargeReviewDataSeeder.class);
    private static final String CONFIRMATION = "RESET_LARGE_COLLEGE_ERP";
    private static final String ACADEMIC_YEAR = "2026-27";
    private static final int STAFF_USERS_PER_COLLEGE = 9;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final String confirmation;
    private final int collegeCount;
    private final int totalUsers;
    private final String demoPassword;

    public LargeReviewDataSeeder(
            JdbcTemplate jdbc,
            PasswordEncoder passwords,
            @Value("${app.large-demo.confirm:}") String confirmation,
            @Value("${app.large-demo.colleges:40}") int collegeCount,
            @Value("${app.large-demo.total-users:200000}") int totalUsers,
            @Value("${app.large-demo.password:}") String demoPassword) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.confirmation = confirmation;
        this.collegeCount = collegeCount;
        this.totalUsers = totalUsers;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        validateConfiguration();
        int studentCount = totalUsers - 1 - (collegeCount * STAFF_USERS_PER_COLLEGE);
        log.warn("Resetting ERP data for large review dataset: colleges={}, users={}, students={}",
                collegeCount, totalUsers, studentCount);

        truncatePublicTables();
        seedRoles();
        String adminHash = passwords.encode(demoPassword);
        String demoHash = passwords.encode(demoPassword);
        seedAdmin(adminHash);
        seedInstitutions();
        seedStaff(demoHash);
        seedAcademics();
        seedStudents(demoHash, studentCount);
        seedAdmissionsAndEnrollments();
        seedFees();
        seedTimetablesAndAttendance();
        seedNoticesAndAudit();
        verifyCounts(studentCount);
        log.warn("Large review dataset ready");
    }

    private void validateConfiguration() {
        if (!CONFIRMATION.equals(confirmation)) {
            throw new IllegalStateException("Large demo reset confirmation is missing");
        }
        if (demoPassword == null || demoPassword.length() < 12) {
            throw new IllegalStateException("LARGE_DEMO_PASSWORD must contain at least 12 characters");
        }
        if (collegeCount < 1 || collegeCount > 100) {
            throw new IllegalArgumentException("Large demo college count must be between 1 and 100");
        }
        int reservedUsers = 1 + (collegeCount * STAFF_USERS_PER_COLLEGE);
        if (totalUsers <= reservedUsers || totalUsers > 1_000_000) {
            throw new IllegalArgumentException("Large demo total users must exceed staff users and be at most 1,000,000");
        }
    }

    private void truncatePublicTables() {
        var tables = jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema='public' and table_type='BASE TABLE'
                  and table_name<>'flyway_schema_history'
                """, String.class);
        if (tables.isEmpty()) return;
        String names = tables.stream()
                .map(name -> "\"" + name.replace("\"", "\"\"") + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        jdbc.execute("TRUNCATE TABLE " + names + " RESTART IDENTITY CASCADE");
    }

    private void seedRoles() {
        for (RoleName role : RoleName.values()) {
            jdbc.update("insert into roles(name,description,created_at,updated_at) values(?,?,now(),now())",
                    role.name(), role.name().replace('_', ' ') + " role");
        }
    }

    private void seedAdmin(String hash) {
        jdbc.update("""
                insert into users(full_name,email,phone,password_hash,must_change_password,email_verified,status,
                                  failed_login_attempts,session_version,created_at,updated_at)
                values('Super Admin','admin@erp.com','9999999999',?,false,true,'ACTIVE',0,0,now(),now())
                """, hash);
        jdbc.update("""
                insert into user_roles(user_id,role_id)
                select u.id,r.id from users u join roles r on r.name='SUPER_ADMIN'
                where u.email='admin@erp.com'
                """);
    }

    private void seedInstitutions() {
        jdbc.update("""
                insert into colleges(name,code,address,city,state,pincode,contact_email,contact_phone,status,created_at,updated_at)
                select 'College ERP College '||lpad(n::text,2,'0'),
                       'COL'||lpad(n::text,2,'0'),
                       'Knowledge Campus '||n,
                       case n%4 when 0 then 'Pune' when 1 then 'Mumbai' when 2 then 'Nashik' else 'Nagpur' end,
                       'Maharashtra', '411'||lpad(n::text,3,'0'),
                       'office.c'||lpad(n::text,2,'0')||'@college-erp.demo',
                       '02025'||lpad(n::text,5,'0'),'ACTIVE',now(),now()
                from generate_series(1,?) n
                """, collegeCount);
        jdbc.update("""
                insert into departments(college_id,name,code,description,status,created_at,updated_at)
                select c.id,d.name,d.code,d.description,'ACTIVE',now(),now()
                from colleges c
                cross join (values
                    ('Bachelor of Computer Applications','BCA','Computer applications programme'),
                    ('Bachelor of Commerce','BCOM','Commerce and management programme')
                ) d(name,code,description)
                """);
    }

    private void seedStaff(String hash) {
        jdbc.execute("""
                create temporary table demo_staff_seed on commit drop as
                select c.id college_id,
                       d.id department_id,
                       spec.key,
                       spec.staff_type,
                       spec.role_name,
                       spec.display_name||' '||c.code full_name,
                       lower(spec.key)||'.'||lower(c.code)||'@college-erp.demo' email,
                       c.code||'-'||spec.employee_suffix employee_code,
                       '8'||lpad((c.id*100+spec.phone_suffix)::text,9,'0') phone
                from colleges c
                cross join (values
                    ('principal',null,null,'PRINCIPAL','Dr. Principal','PRI',1),
                    ('hod.bca','BCA','HOD','HOD','Prof. BCA HOD','HOD-BCA',2),
                    ('hod.bcom','BCOM','HOD','HOD','Prof. BCom HOD','HOD-BCOM',3),
                    ('teacher.a.bca','BCA','CLASS_TEACHER','CLASS_TEACHER','Prof. BCA Teacher A','T-BCA-A',4),
                    ('teacher.b.bca','BCA','CLASS_TEACHER','CLASS_TEACHER','Prof. BCA Teacher B','T-BCA-B',5),
                    ('teacher.a.bcom','BCOM','CLASS_TEACHER','CLASS_TEACHER','Prof. BCom Teacher A','T-BCOM-A',6),
                    ('teacher.b.bcom','BCOM','CLASS_TEACHER','CLASS_TEACHER','Prof. BCom Teacher B','T-BCOM-B',7),
                    ('fee','BCOM','FEE_SECTION','FEE_SECTION','Fee Officer','FEE',8),
                    ('student.section','BCA','STUDENT_SECTION','STUDENT_SECTION','Student Section Officer','STU',9)
                ) spec(key,department_code,staff_type,role_name,display_name,employee_suffix,phone_suffix)
                left join departments d on d.college_id=c.id and d.code=spec.department_code
                """);
        jdbc.update("""
                insert into users(college_id,full_name,email,phone,password_hash,must_change_password,email_verified,status,
                                  failed_login_attempts,session_version,created_at,updated_at)
                select college_id,full_name,email,phone,?,false,true,'ACTIVE',0,0,now(),now()
                from demo_staff_seed
                """, hash);
        jdbc.update("""
                insert into user_roles(user_id,role_id)
                select u.id,r.id from demo_staff_seed s
                join users u on u.email=s.email join roles r on r.name=s.role_name
                """);
        jdbc.update("""
                insert into user_roles(user_id,role_id)
                select u.id,r.id from demo_staff_seed s
                join users u on u.email=s.email cross join roles r
                where s.staff_type='CLASS_TEACHER' and r.name='SUBJECT_TEACHER'
                """);
        jdbc.update("""
                insert into user_roles(user_id,role_id)
                select u.id,r.id from demo_staff_seed s
                join users u on u.email=s.email cross join roles r
                where s.staff_type='HOD' and r.name='SUBJECT_TEACHER'
                """);
        jdbc.update("""
                insert into staff_profiles(user_id,college_id,department_id,employee_code,full_name,email,phone,
                                           staff_type,status,joining_date,created_at,updated_at)
                select u.id,s.college_id,s.department_id,s.employee_code,s.full_name,s.email,s.phone,
                       s.staff_type,'ACTIVE',date '2025-06-01',now(),now()
                from demo_staff_seed s join users u on u.email=s.email
                where s.staff_type is not null
                """);
        jdbc.update("""
                insert into staff_profile_departments(staff_profile_id,department_id)
                select sp.id,sp.department_id from staff_profiles sp where sp.department_id is not null
                """);
        jdbc.update("""
                insert into staff_profile_departments(staff_profile_id,department_id)
                select sp.id,other.id from staff_profiles sp
                join colleges c on c.id=sp.college_id
                join departments other on other.college_id=c.id and other.id<>sp.department_id
                where sp.employee_code like '%-T-BCA-A'
                on conflict do nothing
                """);
    }

    private void seedAcademics() {
        jdbc.update("""
                insert into course_years(college_id,department_id,academic_year,year_name,name,code,description,status,created_at,updated_at)
                select c.id,d.id,?,y.year_name,
                       y.display||' '||d.name,
                       d.code||'-'||y.code,
                       'Large review academic data','ACTIVE',now(),now()
                from colleges c join departments d on d.college_id=c.id
                cross join (values
                    (1,'FIRST_YEAR','First Year','FY'),
                    (2,'SECOND_YEAR','Second Year','SY'),
                    (3,'THIRD_YEAR','Third Year','TY')
                ) y(sequence,year_name,display,code)
                where y.sequence <= case
                    when d.code='BCA' and c.id<=10 then 1
                    when d.code='BCOM' and c.id<=20 then 2
                    else 3 end
                """, ACADEMIC_YEAR);
        jdbc.update("""
                insert into course_year_divisions(college_id,department_id,academic_class_id,academic_year,name,code,
                                                  capacity,status,created_at,updated_at)
                select cy.college_id,cy.department_id,cy.id,cy.academic_year,
                       'Division '||v.code,v.code,3000,'ACTIVE',now(),now()
                from course_years cy cross join (values('A'),('B')) v(code)
                """);
        jdbc.update("""
                update course_year_divisions division set class_teacher_id=teacher.id
                from staff_profiles teacher
                where teacher.college_id=division.college_id
                  and teacher.department_id=division.department_id
                  and teacher.employee_code like case division.code
                      when 'A' then '%-T-'||(select code from departments where id=division.department_id)||'-A'
                      else '%-T-'||(select code from departments where id=division.department_id)||'-B' end
                """);
        jdbc.update("""
                insert into course_year_subjects(college_id,department_id,academic_class_id,academic_year,name,code,
                                                 description,credits,status,subject_type,created_at,updated_at)
                select cy.college_id,cy.department_id,cy.id,cy.academic_year,
                       case d.code
                         when 'BCA' then (array['Programming','Database Management','Computer Networks'])[s.n]
                         else (array['Financial Accounting','Business Economics','Business Communication'])[s.n]
                       end||' - '||replace(cy.year_name,'_',' '),
                       d.code||'-'||substring(cy.year_name,1,1)||s.n,
                       'Large review subject',4,'ACTIVE',
                       case when s.n=3 then 'PRACTICAL' else 'THEORY' end,now(),now()
                from course_years cy join departments d on d.id=cy.department_id
                cross join generate_series(1,3) s(n)
                """);
        jdbc.update("""
                insert into subject_teacher_assignments(subject_id,teacher_id,academic_year,status,assigned_at,created_at,updated_at)
                select subject.id,teacher.id,subject.academic_year,'ACTIVE',now(),now(),now()
                from course_year_subjects subject
                join staff_profiles teacher on teacher.college_id=subject.college_id
                  and teacher.department_id=subject.department_id
                  and teacher.staff_type in ('HOD','CLASS_TEACHER')
                """);
    }

    private void seedStudents(String hash, int studentCount) {
        jdbc.execute("""
                create temporary table demo_student_seed(
                    n integer primary key, college_id bigint, department_id bigint, academic_class_id bigint,
                    section_id bigint, first_name text, last_name text, full_name text, email text, phone text,
                    category text, total_fee numeric, minimum_fee numeric
                ) on commit drop
                """);
        jdbc.update("""
                insert into demo_student_seed
                select n,c.id,d.id,cy.id,division.id,
                       first_names[(n-1)%20+1],last_names[(n-1)%10+1],
                       first_names[(n-1)%20+1]||' '||last_names[(n-1)%10+1],
                       'student'||lpad(n::text,6,'0')||'@college-erp.demo',
                       '7'||lpad(n::text,9,'0'),
                       categories[(n-1)%8+1],
                       case d.code when 'BCA' then 60000 else 50000 end,
                       case d.code when 'BCA' then 15000 else 12000 end
                from generate_series(1,?) n
                cross join (select
                    array['Aarav','Aditi','Arjun','Anaya','Vivaan','Diya','Kabir','Ira','Reyansh','Myra','Atharv','Saanvi','Advik','Kiara','Rudra','Riya','Vihaan','Anvi','Dhruv','Sara'] first_names,
                    array['Patil','Shinde','Kulkarni','Jadhav','Joshi','More','Pawar','Desai','Mehta','Khan'] last_names,
                    array['OPEN','OBC','SC','ST','SBC','VJNT','EWS','OTHER'] categories) names
                join colleges c on c.code='COL'||lpad((((n-1)%?)+1)::text,2,'0')
                join departments d on d.college_id=c.id and d.code=case when n%2=0 then 'BCA' else 'BCOM' end
                join course_years cy on cy.department_id=d.id and cy.year_name='FIRST_YEAR'
                join course_year_divisions division on division.academic_class_id=cy.id
                  and division.code=case when (n/2)%2=0 then 'A' else 'B' end
                """, studentCount, collegeCount);
        jdbc.update("""
                insert into users(college_id,full_name,email,phone,password_hash,must_change_password,email_verified,status,
                                  failed_login_attempts,session_version,created_at,updated_at)
                select college_id,full_name,email,phone,?,false,true,'ACTIVE',0,0,now(),now()
                from demo_student_seed order by n
                """, hash);
        jdbc.update("""
                insert into user_roles(user_id,role_id)
                select u.id,r.id from demo_student_seed s join users u on u.email=s.email
                cross join roles r where r.name='STUDENT'
                """);
        jdbc.update("""
                insert into student_profiles(user_id,college_id,department_id,admission_number,student_category,
                    first_name,last_name,full_name,email,phone,date_of_birth,gender,address_line1,city,state,pincode,
                    parent_name,parent_phone,parent_email,status,roll_number,activated_at,created_at,updated_at)
                select u.id,s.college_id,s.department_id,'JD26'||lpad(s.n::text,7,'0'),s.category,
                    s.first_name,s.last_name,s.full_name,s.email,s.phone,
                    date '2006-01-01'+((s.n%700)),'OTHER','Student Address '||s.n,'Pune','Maharashtra','411038',
                    'Parent of '||s.full_name,'9'||lpad(s.n::text,9,'0'),'parent'||s.n||'@example.com',
                    case s.n%10 when 0 then 'UNDER_REVIEW' when 1 then 'UNDER_REVIEW'
                         when 2 then 'ADMISSION_REJECTED' else 'ACTIVE' end,
                    lpad(s.n::text,7,'0'),
                    case when s.n%10 in (0,1,2) then null else now() end,now(),now()
                from demo_student_seed s join users u on u.email=s.email
                """);
    }

    private void seedAdmissionsAndEnrollments() {
        jdbc.update("""
                insert into admission_forms(admission_reference_number,college_id,department_id,student_id,student_user_id,
                    academic_year,student_category,first_name,last_name,full_name,email,phone,date_of_birth,gender,
                    address_line1,city,state,pincode,parent_name,parent_phone,parent_email,previous_school_name,
                    previous_class_name,previous_percentage,status,source,submitted_at,student_section_verified_at,
                    student_section_verified_by,student_section_remarks,principal_approved_at,details_completed_at,
                    print_count,created_at,updated_at)
                select 'ADM-26-'||lpad(s.n::text,7,'0'),s.college_id,s.department_id,sp.id,u.id,?,s.category,
                    s.first_name,s.last_name,s.full_name,s.email,s.phone,sp.date_of_birth,sp.gender,
                    'Student Address '||s.n,'Pune','Maharashtra','411038','Parent of '||s.full_name,
                    '9'||lpad(s.n::text,9,'0'),'parent'||s.n||'@example.com','Demo Higher Secondary School','12th',
                    60+(s.n%40),
                    case s.n%10 when 0 then 'PRINCIPAL_REVIEW_PENDING'
                         when 1 then 'STUDENT_SECTION_REVIEW_PENDING'
                         when 2 then 'PRINCIPAL_REJECTED' else 'PRINCIPAL_APPROVED' end,
                    'PUBLIC_LINK',now()-interval '30 days',
                    case when s.n%10=1 then null else now()-interval '25 days' end,
                    student_officer.id,'Bulk verification',
                    case when s.n%10 not in (0,1,2) then now()-interval '20 days' end,
                    now()-interval '30 days',case when s.n%10 not in (0,1,2) then 1 else 0 end,now(),now()
                from demo_student_seed s
                join users u on u.email=s.email join student_profiles sp on sp.user_id=u.id
                join users student_officer on student_officer.college_id=s.college_id
                  and student_officer.email like 'student.section.%'
                """, ACADEMIC_YEAR);
        jdbc.update("""
                insert into admission_status_history(admission_form_id,changed_by,old_status,new_status,action,remarks,created_at,updated_at)
                select a.id,principal.id,
                       case a.status
                         when 'STUDENT_SECTION_REVIEW_PENDING' then 'SUBMITTED'
                         when 'PRINCIPAL_REVIEW_PENDING' then 'STUDENT_SECTION_APPROVED'
                         else 'PRINCIPAL_REVIEW_PENDING' end,
                       a.status,
                       case a.status
                         when 'STUDENT_SECTION_REVIEW_PENDING' then 'STUDENT_SECTION_REVIEW_STARTED'
                         when 'PRINCIPAL_REVIEW_PENDING' then 'PRINCIPAL_REVIEW_PENDING'
                         when 'PRINCIPAL_REJECTED' then 'PRINCIPAL_REJECTED'
                         else 'PRINCIPAL_APPROVED' end,
                       'Large review workflow state',now(),now()
                from admission_forms a join users principal on principal.college_id=a.college_id
                  and principal.email like 'principal.%'
                """);
        jdbc.update("""
                insert into student_section_enrollments(student_id,section_id,academic_class_id,academic_year,
                                                        roll_number,status,enrolled_at,created_at,updated_at)
                select sp.id,s.section_id,s.academic_class_id,?,lpad(s.n::text,7,'0'),'ACTIVE',now(),now(),now()
                from demo_student_seed s join users u on u.email=s.email
                join student_profiles sp on sp.user_id=u.id join admission_forms a on a.student_id=sp.id
                where a.status='PRINCIPAL_APPROVED'
                """, ACADEMIC_YEAR);
    }

    private void seedFees() {
        jdbc.update("""
                insert into fee_structures(college_id,department_id,academic_year,student_category,title,description,
                    total_fee,minimum_amount_for_admission,admission_fee,tuition_fee,exam_fee,library_fee,other_fee,
                    status,created_at,updated_at)
                select d.college_id,d.id,?,'OPEN',d.name||' Fees','Large review fee structure',
                    case d.code when 'BCA' then 60000 else 50000 end,
                    case d.code when 'BCA' then 15000 else 12000 end,
                    5000,case d.code when 'BCA' then 45000 else 35000 end,3000,2000,5000,'ACTIVE',now(),now()
                from departments d
                """, ACADEMIC_YEAR);
        jdbc.update("""
                insert into student_fee_accounts(student_id,student_user_id,admission_form_id,college_id,department_id,
                    fee_structure_id,academic_year,student_category,total_fee,paid_amount,remaining_amount,
                    discount_amount,minimum_amount_for_admission,status,created_at,updated_at)
                select sp.id,u.id,a.id,s.college_id,s.department_id,fs.id,?,s.category,s.total_fee,
                    case s.n%3 when 0 then s.total_fee when 1 then s.total_fee/2 else 0 end,
                    case s.n%3 when 0 then 0 when 1 then s.total_fee/2 else s.total_fee end,
                    0,s.minimum_fee,
                    case s.n%3 when 0 then 'PAID' when 1 then 'PARTIALLY_PAID' else 'PENDING' end,
                    now(),now()
                from demo_student_seed s join users u on u.email=s.email
                join student_profiles sp on sp.user_id=u.id join admission_forms a on a.student_id=sp.id
                join fee_structures fs on fs.college_id=s.college_id and fs.department_id=s.department_id
                where a.status='PRINCIPAL_APPROVED'
                """, ACADEMIC_YEAR);
        jdbc.update("""
                insert into fee_payments(fee_account_id,student_id,student_user_id,college_id,department_id,amount,
                    payment_mode,transaction_reference,payment_date,proof_url,remarks,status,submitted_at,
                    verified_at,verified_by_id,rejected_at,rejected_by_id,rejection_reason,created_at,updated_at)
                select fa.id,fa.student_id,fa.student_user_id,fa.college_id,fa.department_id,
                    case when fa.paid_amount>0 then fa.paid_amount else fa.minimum_amount_for_admission end,
                    case s.n%2 when 0 then 'UPI' else 'BANK_TRANSFER' end,
                    'LOAD-'||lpad(s.n::text,7,'0'),current_date-(s.n%30),'/demo/payment-'||s.n||'.png',
                    'Large review payment',
                    case when fa.paid_amount>0 then 'VERIFIED' when s.n%2=0 then 'PENDING' else 'REJECTED' end,
                    now()-interval '5 days',
                    case when fa.paid_amount>0 then now()-interval '4 days' end,
                    case when fa.paid_amount>0 then fee_user.id end,
                    case when fa.paid_amount=0 and s.n%2=1 then now()-interval '3 days' end,
                    case when fa.paid_amount=0 and s.n%2=1 then fee_user.id end,
                    case when fa.paid_amount=0 and s.n%2=1 then 'Demo proof rejected' end,now(),now()
                from student_fee_accounts fa join student_profiles sp on sp.id=fa.student_id
                join demo_student_seed s on s.email=sp.email
                join users fee_user on fee_user.college_id=fa.college_id and fee_user.email like 'fee.%'
                """);
        jdbc.update("""
                insert into fee_transactions(fee_account_id,fee_payment_id,transaction_type,amount,
                    previous_paid_amount,new_paid_amount,previous_remaining_amount,new_remaining_amount,
                    remarks,performed_by_id,created_at,updated_at)
                select p.fee_account_id,p.id,'PAYMENT_VERIFIED',p.amount,0,p.amount,
                    a.total_fee,a.total_fee-p.amount,'Bulk verified payment',p.verified_by_id,now(),now()
                from fee_payments p join student_fee_accounts a on a.id=p.fee_account_id
                where p.status='VERIFIED'
                """);
    }

    private void seedTimetablesAndAttendance() {
        jdbc.update("""
                insert into weekly_timetables(college_id,section_id,status,created_at,updated_at)
                select division.college_id,division.id,'ACTIVE',now(),now()
                from course_year_divisions division join course_years cy on cy.id=division.academic_class_id
                where cy.year_name='FIRST_YEAR'
                """);
        jdbc.update("""
                insert into weekly_timetable_periods(timetable_id,position,label,start_time,end_time,kind,created_at,updated_at)
                select wt.id,p.position,p.label,p.start_time::time,p.end_time::time,p.kind,now(),now()
                from weekly_timetables wt cross join (values
                    (1,'Period 1','08:30','09:20','TEACHING'),
                    (2,'Period 2','09:20','10:10','TEACHING'),
                    (3,'Short Break','10:10','10:25','SHORT_BREAK'),
                    (4,'Period 3','10:25','11:15','TEACHING'),
                    (5,'Lunch Break','11:15','12:00','LUNCH_BREAK'),
                    (6,'Period 4','12:00','12:50','TEACHING')
                ) p(position,label,start_time,end_time,kind)
                """);
        jdbc.update("""
                insert into weekly_timetable_entries(timetable_id,day_of_week,period_id,subject_id,teacher_id,
                                                     room,lecture_type,remarks,created_at,updated_at)
                select wt.id,day.name,period.id,subject.id,division.class_teacher_id,
                       'R-'||division.id,subject.subject_type,'Large review lecture',now(),now()
                from weekly_timetables wt join course_year_divisions division on division.id=wt.section_id
                join weekly_timetable_periods period on period.timetable_id=wt.id and period.kind='TEACHING'
                cross join (values('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY')) day(name)
                join lateral (
                    select s.id,s.subject_type from course_year_subjects s
                    where s.academic_class_id=division.academic_class_id
                    order by s.id offset ((period.position-1)%3) limit 1
                ) subject on true
                """);
        jdbc.update("""
                insert into weekly_attendance_sessions(college_id,timetable_entry_id,section_id,subject_id,teacher_id,
                    attendance_date,start_time,end_time,lecture_number,status,submitted_at,created_at,updated_at)
                select wt.college_id,entry.id,wt.section_id,entry.subject_id,entry.teacher_id,
                       current_date-(week.n*7),period.start_time,period.end_time,1,'SUBMITTED',now(),now(),now()
                from weekly_timetables wt
                join weekly_timetable_entries entry on entry.timetable_id=wt.id and entry.day_of_week='MONDAY'
                join weekly_timetable_periods period on period.id=entry.period_id and period.position=1
                cross join generate_series(1,4) week(n)
                """);
        jdbc.update("""
                insert into weekly_attendance_records(session_id,student_id,status,remarks,created_at,updated_at)
                select session.id,enrollment.student_id,
                       case (enrollment.student_id+extract(day from session.attendance_date)::int)%20
                         when 0 then 'ABSENT' when 1 then 'LATE' when 2 then 'LEAVE' else 'PRESENT' end,
                       case when (enrollment.student_id+extract(day from session.attendance_date)::int)%20=0
                         then 'Absent in large review history' end,now(),now()
                from weekly_attendance_sessions session
                join student_section_enrollments enrollment on enrollment.section_id=session.section_id
                """);
    }

    private void seedNoticesAndAudit() {
        jdbc.update("""
                insert into notices(title,message,created_by_user_id,created_at,updated_at)
                select 'Welcome to '||c.name,
                       'Large review dataset with admissions, fees, timetables and attendance.',
                       principal.id,now(),now()
                from colleges c join users principal on principal.college_id=c.id and principal.email like 'principal.%'
                """);
        jdbc.update("""
                insert into notice_colleges(notice_id,college_id)
                select n.id,u.college_id from notices n join users u on u.id=n.created_by_user_id
                """);
        jdbc.update("""
                insert into notice_audience_roles(notice_id,role_name)
                select n.id,role_name from notices n
                cross join (values('HOD'),('CLASS_TEACHER'),('SUBJECT_TEACHER'),('STUDENT')) audience(role_name)
                """);
        jdbc.update("""
                insert into erp_audit_logs(college_id,action,entity_type,entity_id,actor_id,details,created_at,updated_at)
                select c.id,'LARGE_REVIEW_DATA_CREATED','College',c.id,admin.id,
                       'Generated large workflow review dataset',now(),now()
                from colleges c cross join users admin where admin.email='admin@erp.com'
                """);
    }

    private void verifyCounts(int expectedStudents) {
        int actualColleges = count("colleges");
        int actualUsers = count("users");
        int actualStudents = count("student_profiles");
        if (actualColleges != collegeCount || actualUsers != totalUsers || actualStudents != expectedStudents) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "Large dataset count mismatch: colleges=%d/%d users=%d/%d students=%d/%d",
                    actualColleges, collegeCount, actualUsers, totalUsers, actualStudents, expectedStudents));
        }
        log.warn("Verified large dataset: colleges={}, users={}, students={}, feeAccounts={}, attendanceRecords={}",
                actualColleges, actualUsers, actualStudents, count("student_fee_accounts"),
                count("weekly_attendance_records"));
    }

    private int count(String table) {
        Integer count = jdbc.queryForObject("select count(*) from " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
