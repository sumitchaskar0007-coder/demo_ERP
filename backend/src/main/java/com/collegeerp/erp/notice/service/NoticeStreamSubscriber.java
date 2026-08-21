package com.collegeerp.erp.notice.service;

import com.collegeerp.erp.user.entity.RoleName;

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
