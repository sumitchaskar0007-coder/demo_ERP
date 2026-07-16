package com.jadhavr.erp.notice.repository;

import com.jadhavr.erp.notice.entity.Notice;
import com.jadhavr.erp.user.entity.RoleName;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @EntityGraph(attributePaths = {"createdBy", "colleges", "department", "audienceRoles"})
    List<Notice> findByCreatedByIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "colleges", "department", "audienceRoles"})
    @Query("""
            select distinct n from Notice n
            join n.audienceRoles role
            left join n.colleges college
            where n.createdBy.id <> :userId
              and role in :roles
              and (college is null or college.id = :collegeId)
              and (n.department is null or n.department.id = :departmentId)
            order by n.createdAt desc
            """)
    List<Notice> findInbox(
            @Param("userId") Long userId,
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("roles") Collection<RoleName> roles,
            Pageable pageable);
}
