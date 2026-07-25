package com.jadhavr.erp.analytics.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AdminAnalyticsReadRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public AdminAnalyticsReadRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Cacheable(cacheNames = "adminAnalytics",
            key = "'c:'+(#collegeId?:'all')+':d:'+(#departmentId?:'all')+':y:'+(#courseYearId?:'all')+':v:'+(#divisionId?:'all')",
            sync = true)
    public Map<String, Object> read(Long collegeId, Long departmentId, Long courseYearId, Long divisionId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("collegeId", collegeId, Types.BIGINT)
                .addValue("departmentId", departmentId, Types.BIGINT)
                .addValue("courseYearId", courseYearId, Types.BIGINT)
                .addValue("divisionId", divisionId, Types.BIGINT);

        Map<String, Object> summaryRow = jdbc.queryForMap("""
                select
                  (select count(*) from colleges c where (:collegeId is null or c.id=:collegeId)) total_colleges,
                  (select count(*) from colleges c where c.status='ACTIVE' and (:collegeId is null or c.id=:collegeId)) active_colleges,
                  (select count(distinct u.id) from users u join user_roles ur on ur.user_id=u.id
                    join roles r on r.id=ur.role_id and r.name='PRINCIPAL'
                    where (:collegeId is null or u.college_id=:collegeId)) total_principals,
                  (select count(distinct sp.id) from staff_profiles sp
                    where (:collegeId is null or sp.college_id=:collegeId)
                      and (:departmentId is null or sp.department_id=:departmentId or exists(
                        select 1 from staff_profile_departments spd
                        where spd.staff_profile_id=sp.id and spd.department_id=:departmentId))) total_staff,
                  (select count(*) from student_profiles s
                    where (:collegeId is null or s.college_id=:collegeId)
                      and (:departmentId is null or s.department_id=:departmentId)
                      and ((:courseYearId is null and :divisionId is null) or exists(
                        select 1 from student_section_enrollments e
                        where e.student_id=s.id and e.status='ACTIVE'
                          and (:courseYearId is null or e.academic_class_id=:courseYearId)
                          and (:divisionId is null or e.section_id=:divisionId)))) total_students,
                  (select coalesce(sum(a.paid_amount),0) from student_fee_accounts a
                    where (:collegeId is null or a.college_id=:collegeId)
                      and (:departmentId is null or a.department_id=:departmentId)
                      and ((:courseYearId is null and :divisionId is null) or exists(
                        select 1 from student_section_enrollments e
                        where e.student_id=a.student_id and e.status='ACTIVE'
                          and (:courseYearId is null or e.academic_class_id=:courseYearId)
                          and (:divisionId is null or e.section_id=:divisionId)))) total_fee_collection,
                  (select coalesce(sum(a.remaining_amount),0) from student_fee_accounts a
                    where (:collegeId is null or a.college_id=:collegeId)
                      and (:departmentId is null or a.department_id=:departmentId)
                      and ((:courseYearId is null and :divisionId is null) or exists(
                        select 1 from student_section_enrollments e
                        where e.student_id=a.student_id and e.status='ACTIVE'
                          and (:courseYearId is null or e.academic_class_id=:courseYearId)
                          and (:divisionId is null or e.section_id=:divisionId)))) pending_fee
                """, parameters);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalColleges", number(summaryRow.get("total_colleges")));
        summary.put("activeColleges", number(summaryRow.get("active_colleges")));
        summary.put("totalPrincipals", number(summaryRow.get("total_principals")));
        summary.put("totalStaff", number(summaryRow.get("total_staff")));
        summary.put("totalStudents", number(summaryRow.get("total_students")));
        summary.put("totalFeeCollection", decimal(summaryRow.get("total_fee_collection")));
        summary.put("pendingFee", decimal(summaryRow.get("pending_fee")));

        List<Map<String, Object>> collegeWiseStudents = jdbc.query("""
                select c.name label,count(s.id) value from student_profiles s
                join colleges c on c.id=s.college_id
                where (:collegeId is null or s.college_id=:collegeId)
                  and (:departmentId is null or s.department_id=:departmentId)
                  and ((:courseYearId is null and :divisionId is null) or exists(
                    select 1 from student_section_enrollments e where e.student_id=s.id and e.status='ACTIVE'
                      and (:courseYearId is null or e.academic_class_id=:courseYearId)
                      and (:divisionId is null or e.section_id=:divisionId)))
                group by c.id,c.name order by c.name
                """, parameters, (rs, row) -> Map.of("label", rs.getString("label"), "value", rs.getLong("value")));

        List<Map<String, Object>> collegeWiseFees = jdbc.query("""
                select c.name label,coalesce(sum(a.paid_amount),0) value from student_fee_accounts a
                join colleges c on c.id=a.college_id
                where (:collegeId is null or a.college_id=:collegeId)
                  and (:departmentId is null or a.department_id=:departmentId)
                  and ((:courseYearId is null and :divisionId is null) or exists(
                    select 1 from student_section_enrollments e where e.student_id=a.student_id and e.status='ACTIVE'
                      and (:courseYearId is null or e.academic_class_id=:courseYearId)
                      and (:divisionId is null or e.section_id=:divisionId)))
                group by c.id,c.name order by c.name
                """, parameters, (rs, row) -> Map.of("label", rs.getString("label"), "value", rs.getBigDecimal("value")));

        List<Map<String, Object>> feeCollectionTrend = jdbc.query("""
                select to_char(date_trunc('month', p.payment_date), 'Mon YYYY') label,
                       coalesce(sum(p.amount),0) value
                from fee_payments p
                where p.status='VERIFIED'
                  and (:collegeId is null or p.college_id=:collegeId)
                  and (:departmentId is null or p.department_id=:departmentId)
                  and ((:courseYearId is null and :divisionId is null) or exists(
                    select 1 from student_section_enrollments e where e.student_id=p.student_id and e.status='ACTIVE'
                      and (:courseYearId is null or e.academic_class_id=:courseYearId)
                      and (:divisionId is null or e.section_id=:divisionId)))
                group by date_trunc('month', p.payment_date)
                order by date_trunc('month', p.payment_date)
                """, parameters, (rs, row) -> Map.of(
                "label", rs.getString("label"),
                "value", rs.getBigDecimal("value")));

        List<Map<String, Object>> departmentWiseStudents = jdbc.query("""
                select d.name label,count(s.id) value
                from student_profiles s
                join departments d on d.id=s.department_id
                where (:collegeId is null or s.college_id=:collegeId)
                  and (:departmentId is null or s.department_id=:departmentId)
                  and ((:courseYearId is null and :divisionId is null) or exists(
                    select 1 from student_section_enrollments e where e.student_id=s.id and e.status='ACTIVE'
                      and (:courseYearId is null or e.academic_class_id=:courseYearId)
                      and (:divisionId is null or e.section_id=:divisionId)))
                group by d.id,d.name order by d.name
                """, parameters, (rs, row) -> Map.of(
                "label", rs.getString("label"),
                "value", rs.getLong("value")));

        Map<String, Long> admissionDistribution = new LinkedHashMap<>();
        jdbc.query("""
                select a.status,count(*) value from admission_forms a
                where (:collegeId is null or a.college_id=:collegeId)
                  and (:departmentId is null or a.department_id=:departmentId)
                  and ((:courseYearId is null and :divisionId is null) or exists(
                    select 1 from student_section_enrollments e where e.student_id=a.student_id and e.status='ACTIVE'
                      and (:courseYearId is null or e.academic_class_id=:courseYearId)
                      and (:divisionId is null or e.section_id=:divisionId)))
                group by a.status order by a.status
                """, parameters, (ResultSetExtractor<Void>) rs -> {
            while (rs.next()) admissionDistribution.put(rs.getString("status"), rs.getLong("value"));
            return null;
        });

        List<Map<String, Object>> pendingFees = jdbc.query("""
                select s.full_name student,c.name college,a.remaining_amount remaining
                from student_fee_accounts a join student_profiles s on s.id=a.student_id
                join colleges c on c.id=a.college_id
                where a.remaining_amount>0
                  and (:collegeId is null or a.college_id=:collegeId)
                  and (:departmentId is null or a.department_id=:departmentId)
                  and ((:courseYearId is null and :divisionId is null) or exists(
                    select 1 from student_section_enrollments e where e.student_id=a.student_id and e.status='ACTIVE'
                      and (:courseYearId is null or e.academic_class_id=:courseYearId)
                      and (:divisionId is null or e.section_id=:divisionId)) )
                order by a.remaining_amount desc,a.id desc limit 10
                """, parameters, (rs, row) -> Map.of(
                "student", rs.getString("student"),
                "college", rs.getString("college"),
                "remaining", rs.getBigDecimal("remaining")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("collegeWiseStudents", collegeWiseStudents);
        result.put("collegeWiseFeeCollection", collegeWiseFees);
        result.put("feeCollectionTrend", feeCollectionTrend);
        result.put("departmentWiseStudents", departmentWiseStudents);
        result.put("admissionStatusDistribution", admissionDistribution);
        result.put("pendingFees", pendingFees);
        return result;
    }

    private long number(Object value) { return ((Number) value).longValue(); }
    private BigDecimal decimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}
