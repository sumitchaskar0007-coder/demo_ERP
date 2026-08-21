package com.collegeerp.erp.admission.entity;

import com.collegeerp.erp.admission.enums.AdmissionAction;
import com.collegeerp.erp.admission.enums.AdmissionStatus;
import com.collegeerp.erp.common.entity.BaseAuditEntity;
import com.collegeerp.erp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "admission_status_history")
public class AdmissionStatusHistory extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_form_id", nullable = false)
    private AdmissionForm admissionForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AdmissionStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdmissionStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AdmissionAction action;

    @Column(length = 1000)
    private String remarks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AdmissionForm getAdmissionForm() { return admissionForm; }
    public void setAdmissionForm(AdmissionForm admissionForm) { this.admissionForm = admissionForm; }
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }
    public AdmissionStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(AdmissionStatus oldStatus) { this.oldStatus = oldStatus; }
    public AdmissionStatus getNewStatus() { return newStatus; }
    public void setNewStatus(AdmissionStatus newStatus) { this.newStatus = newStatus; }
    public AdmissionAction getAction() { return action; }
    public void setAction(AdmissionAction action) { this.action = action; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
