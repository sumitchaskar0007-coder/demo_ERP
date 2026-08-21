package com.collegeerp.erp.auth.password;

@FunctionalInterface
public interface InitialPasswordPolicy {
    String create(String phone);
}
