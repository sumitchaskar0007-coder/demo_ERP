package com.jadhavr.erp.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "app.rate-limit.http")
public class RateLimitProperties {
    @NotNull private Duration window = Duration.ofMinutes(1);
    @Min(1024) private int maxAuthBodyBytes = 65_536;
    @Valid private Policy login = new Policy(20, 6);
    @Valid private Policy signup = new Policy(10, 10);
    @Valid private Policy password = new Policy(10, 5);
    @Valid private Policy refresh = new Policy(60, 30);
    @Valid private Policy otherAuth = new Policy(20, 10);
    @Valid private Policy publicEndpoints = new Policy(120, 120);
    @Valid private Policy authenticated = new Policy(600, 600);
    @Valid private Backoff backoff = new Backoff();

    public Duration getWindow() { return window; }
    public void setWindow(Duration value) { window = positive(value, "window"); }
    public int getMaxAuthBodyBytes() { return maxAuthBodyBytes; }
    public void setMaxAuthBodyBytes(int value) { maxAuthBodyBytes = value; }
    public Policy getLogin() { return login; }
    public void setLogin(Policy value) { login = value; }
    public Policy getSignup() { return signup; }
    public void setSignup(Policy value) { signup = value; }
    public Policy getPassword() { return password; }
    public void setPassword(Policy value) { password = value; }
    public Policy getRefresh() { return refresh; }
    public void setRefresh(Policy value) { refresh = value; }
    public Policy getOtherAuth() { return otherAuth; }
    public void setOtherAuth(Policy value) { otherAuth = value; }
    public Policy getPublicEndpoints() { return publicEndpoints; }
    public void setPublicEndpoints(Policy value) { publicEndpoints = value; }
    public Policy getAuthenticated() { return authenticated; }
    public void setAuthenticated(Policy value) { authenticated = value; }
    public Backoff getBackoff() { return backoff; }
    public void setBackoff(Backoff value) { backoff = value; }

    @AssertTrue(message = "rate-limit backoff must satisfy base-delay <= max-delay <= reset-after")
    public boolean isBackoffRangeValid() {
        return backoff != null && backoff.baseDelay != null && backoff.maxDelay != null
                && backoff.resetAfter != null
                && backoff.baseDelay.compareTo(backoff.maxDelay) <= 0
                && backoff.maxDelay.compareTo(backoff.resetAfter) <= 0;
    }

    public static class Policy {
        @Min(1) private long perIp;
        @Min(1) private long perAccount;
        public Policy() {}
        public Policy(long perIp, long perAccount) {
            this.perIp = perIp;
            this.perAccount = perAccount;
        }
        public long getPerIp() { return perIp; }
        public void setPerIp(long value) { perIp = value; }
        public long getPerAccount() { return perAccount; }
        public void setPerAccount(long value) { perAccount = value; }
    }

    public static class Backoff {
        @NotNull private Duration baseDelay = Duration.ofSeconds(1);
        @NotNull private Duration maxDelay = Duration.ofMinutes(5);
        @NotNull private Duration resetAfter = Duration.ofMinutes(30);
        public Duration getBaseDelay() { return baseDelay; }
        public void setBaseDelay(Duration value) { baseDelay = positive(value, "base-delay"); }
        public Duration getMaxDelay() { return maxDelay; }
        public void setMaxDelay(Duration value) { maxDelay = positive(value, "max-delay"); }
        public Duration getResetAfter() { return resetAfter; }
        public void setResetAfter(Duration value) { resetAfter = positive(value, "reset-after"); }
    }

    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("Rate-limit " + name + " must be positive");
        }
        return value;
    }
}
