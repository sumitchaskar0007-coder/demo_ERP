package com.jadhavr.erp.notice.repository;

import com.jadhavr.erp.notice.entity.NoticeView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Set;

public interface NoticeViewRepository extends JpaRepository<NoticeView, Long> {
    @Query("select v.notice.id from NoticeView v where v.user.id = :userId")
    Set<Long> findNoticeIdsByUserId(@Param("userId") Long userId);
}
