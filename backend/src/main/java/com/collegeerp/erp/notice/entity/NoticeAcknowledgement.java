package com.collegeerp.erp.notice.entity;

import com.collegeerp.erp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notice_acknowledgements", uniqueConstraints =
        @UniqueConstraint(name = "uk_notice_ack_user", columnNames = {"notice_id", "user_id"}))
public class NoticeAcknowledgement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "acknowledged_at", nullable = false)
    private LocalDateTime acknowledgedAt;

    public User getUser() { return user; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setNotice(Notice value) { notice = value; }
    public void setUser(User value) { user = value; }
    public void setAcknowledgedAt(LocalDateTime value) { acknowledgedAt = value; }
}
