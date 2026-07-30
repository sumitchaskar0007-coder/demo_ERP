package com.jadhavr.erp.auth.security;

import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.time.LocalDateTime;

public class CustomUserDetails implements UserDetails {
    private final Long id;
    private final Long collegeId;
    private final String email;
    private final String password;
    private final String fullName;
    private final UserStatus status;
    private final LocalDateTime lockedUntil;
    private final long sessionVersion;
    private final boolean mustChangePassword;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        id = user.getId();
        collegeId = user.getCollege() == null ? null : user.getCollege().getId();
        email = user.getEmail();
        password = user.getPasswordHash();
        fullName = user.getFullName();
        status = user.getStatus();
        lockedUntil = user.getLockedUntil();
        sessionVersion = user.getSessionVersion();
        mustChangePassword = user.isMustChangePassword();
        authorities = user.getRoles().stream().flatMap(role -> Stream.concat(
                        Stream.of(new SimpleGrantedAuthority("ROLE_" + role.getName().name())),
                        permissions(role.getName()).stream().map(SimpleGrantedAuthority::new)))
                .map(GrantedAuthority.class::cast).toList();
    }
    public CustomUserDetails(AuthorizationSnapshot snapshot) {
        id = snapshot.userId();
        collegeId = snapshot.collegeId();
        email = snapshot.email();
        password = "";
        fullName = null;
        status = snapshot.status();
        lockedUntil = snapshot.lockedUntil();
        sessionVersion = snapshot.sessionVersion();
        mustChangePassword = false;
        authorities = snapshot.authorities().stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
    public Long getId() { return id; }
    public Long getCollegeId() { return collegeId; }
    public String getFullName() { return fullName; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public long getSessionVersion() { return sessionVersion; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now()); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return status == UserStatus.ACTIVE; }
    private List<String> permissions(com.jadhavr.erp.user.entity.RoleName role) {
        return switch (role) {
            case SUPER_ADMIN, ADMIN -> List.of("PERM_NOTICE_READ", "PERM_NOTICE_SEND", "PERM_TENANT_ADMIN", "PERM_TIMETABLE_MANAGE", "PERM_ATTENDANCE_MANAGE");
            case PRINCIPAL, HOD -> List.of("PERM_NOTICE_READ", "PERM_NOTICE_SEND");
            default -> List.of("PERM_NOTICE_READ");
        };
    }
}
