package com.jadhavr.erp.auth.password;

@FunctionalInterface
public interface InitialPasswordPolicy {
    String create(String phone);
}
