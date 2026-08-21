package com.collegeerp.erp.notice.repository;

import com.collegeerp.erp.notice.entity.NoticeView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Set;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface NoticeViewRepository extends JpaRepository<NoticeView, Long> {
    @Query("select v.notice.id from NoticeView v where v.user.id = :userId")
    Set<Long> findNoticeIdsByUserId(@Param("userId") Long userId);
    @EntityGraph(attributePaths = {"user"})
    List<NoticeView> findByNoticeIdOrderBySeenAtDesc(Long noticeId);
}
