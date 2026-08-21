package com.collegeerp.erp.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AdminCacheInvalidationAspect {
    private final ApplicationEventPublisher events;

    public AdminCacheInvalidationAspect(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Pointcut("within(com.collegeerp.erp.college.service..*) || within(com.collegeerp.erp.department.service..*) || "
            + "within(com.collegeerp.erp.academic.service..*) || within(com.collegeerp.erp.admission.service..*) || "
            + "within(com.collegeerp.erp.staff.service..*) || within(com.collegeerp.erp.student.service..*) || "
            + "within(com.collegeerp.erp.fee.service..*) || within(com.collegeerp.erp.timetable.service..*) || "
            + "within(com.collegeerp.erp.attendance.service..*) || within(com.collegeerp.erp.notice.service..*)")
    void cachedDomainService() {}

    @Pointcut("execution(public * create*(..)) || execution(public * update*(..)) || "
            + "execution(public * delete*(..)) || execution(public * setStatus*(..)) || "
            + "execution(public * activate*(..)) || execution(public * deactivate*(..)) || "
            + "execution(public * assign*(..)) || execution(public * remove*(..)) || "
            + "execution(public * save*(..)) || execution(public * verify*(..)) || "
            + "execution(public * reject*(..)) || execution(public * approve*(..)) || "
            + "execution(public * submit*(..)) || execution(public * mark*(..)) || "
            + "execution(public * move*(..)) || execution(public * copy*(..)) || execution(public * replace*(..))")
    void mutationMethod() {}

    @AfterReturning("cachedDomainService() && mutationMethod()")
    public void publishAfterMutation(JoinPoint joinPoint) {
        events.publishEvent(new AdminDataChangedEvent(joinPoint.getSignature().toShortString()));
    }
}
