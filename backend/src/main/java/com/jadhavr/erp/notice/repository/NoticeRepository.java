package com.jadhavr.erp.notice.repository;

import com.jadhavr.erp.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {}
