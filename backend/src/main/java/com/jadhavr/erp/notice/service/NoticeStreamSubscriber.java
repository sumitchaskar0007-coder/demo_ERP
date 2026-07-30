package com.jadhavr.erp.notice.service;

import com.jadhavr.erp.user.entity.RoleName;

import java.util.Set;

record NoticeStreamSubscriber(
        Long userId,
        Long collegeId,
        Long departmentId,
        Set<RoleName> roles) {

    NoticeStreamSubscriber {
        roles = Set.copyOf(roles);
    }
}
