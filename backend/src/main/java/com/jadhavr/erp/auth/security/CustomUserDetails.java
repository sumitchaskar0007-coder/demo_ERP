package com.jadhavr.erp.auth.security;

import com.jadhavr.erp.user.entity.User;
import com.jadhavr.erp.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final Long id;
    private final Long collegeId;
    private final String email;
    private final String password;
    private final String fullName;
    private final UserStatus status;
    private final boolean mustChangePassword;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        id = user.getId();
        collegeId = user.getCollege() == null ? null : user.getCollege().getId();
        email = user.getEmail();
        password = user.getPasswordHash();
        fullName = user.getFullName();
        status = user.getStatus();
        mustChangePassword = user.isMustChangePassword();
        authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .map(GrantedAuthority.class::cast).toList();
    }
    public Long getId() { return id; }
    public Long getCollegeId() { return collegeId; }
    public String getFullName() { return fullName; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return status == UserStatus.ACTIVE; }
}
