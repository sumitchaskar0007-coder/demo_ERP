package com.jadhavr.erp.notice.repository;

import com.jadhavr.erp.notice.entity.NoticeAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Set;

public interface NoticeAcknowledgementRepository extends JpaRepository<NoticeAcknowledgement, Long> {
    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    @Query("select a.notice.id from NoticeAcknowledgement a where a.user.id = :userId")
    Set<Long> findNoticeIdsByUserId(@Param("userId") Long userId);
}
