package com.collegeerp.erp.notice.entity;

import com.collegeerp.erp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notice_views", uniqueConstraints =
        @UniqueConstraint(name = "uk_notice_view_user", columnNames = {"notice_id", "user_id"}))
public class NoticeView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "seen_at", nullable = false)
    private LocalDateTime seenAt;

    public User getUser() { return user; }
    public LocalDateTime getSeenAt() { return seenAt; }
    public void setNotice(Notice notice) { this.notice = notice; }
    public void setUser(User user) { this.user = user; }
    public void setSeenAt(LocalDateTime seenAt) { this.seenAt = seenAt; }
}
