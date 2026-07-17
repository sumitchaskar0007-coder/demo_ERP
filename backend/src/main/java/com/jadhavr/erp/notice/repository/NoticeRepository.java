package com.jadhavr.erp.notice.repository;

import com.jadhavr.erp.notice.entity.Notice;
import com.jadhavr.erp.user.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @EntityGraph(attributePaths = {"createdBy", "colleges", "department", "audienceRoles"})
    @Query("""
            select distinct n from Notice n
            join n.audienceRoles audience
            left join n.colleges college
            where n.deletedAt is null
              and n.createdBy.id <> :userId
              and audience in :roles
              and (n.colleges is empty or college.id = :collegeId)
              and (n.department is null or n.department.id = :departmentId)
            order by n.createdAt desc
            """)
    List<Notice> findInbox(@Param("userId") Long userId,
            @Param("roles") Set<RoleName> roles,
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId);

    @EntityGraph(attributePaths = {"createdBy", "colleges", "department", "audienceRoles"})
    List<Notice> findByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
}
