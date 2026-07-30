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
    @Query("""
            select n.id from Notice n
            where n.createdBy.id = :userId
              and n.deletedAt is null
            order by n.createdAt desc, n.id desc
            """)
    List<Long> findSentIds(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy", "colleges", "department", "audienceRoles"})
    @Query("select distinct n from Notice n where n.id in :ids")
    List<Notice> findDetailedByIdIn(@Param("ids") Collection<Long> ids);

    @Query("""
            select n.id from Notice n
            join n.audienceRoles role
            left join n.colleges college
            where n.createdBy.id <> :userId
              and n.deletedAt is null
              and role in :roles
              and (college is null or college.id = :collegeId)
              and (n.department is null or n.department.id = :departmentId)
            group by n.id, n.createdAt
            order by n.createdAt desc, n.id desc
            """)
    List<Long> findInboxIds(
            @Param("userId") Long userId,
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("roles") Collection<RoleName> roles,
            Pageable pageable);

    @Query("""
            select count(distinct n.id) from Notice n
            join n.audienceRoles role
            left join n.colleges college
            where n.createdBy.id <> :userId
              and n.deletedAt is null
              and role in :roles
              and (college is null or college.id = :collegeId)
              and (n.department is null or n.department.id = :departmentId)
              and not exists (
                  select v.id from NoticeView v
                  where v.notice.id = n.id and v.user.id = :userId
              )
            """)
    long countUnread(
            @Param("userId") Long userId,
            @Param("collegeId") Long collegeId,
            @Param("departmentId") Long departmentId,
            @Param("roles") Collection<RoleName> roles);
}
