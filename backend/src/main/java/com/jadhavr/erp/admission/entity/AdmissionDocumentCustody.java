package com.jadhavr.erp.admission.entity;

import com.jadhavr.erp.common.entity.BaseAuditEntity;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admission_document_custody",
        uniqueConstraints = @UniqueConstraint(name = "uk_admission_document_custody",
                columnNames = {"admission_form_id", "document_type"}))
public class AdmissionDocumentCustody extends BaseAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admission_form_id", nullable = false) private AdmissionForm admissionForm;
    @Column(name = "document_type", nullable = false, length = 60) private String documentType;
    @Column(name = "original_received", nullable = false) private boolean originalReceived;
    @Column(name = "xerox_received", nullable = false) private boolean xeroxReceived;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "received_by") private User receivedBy;
    @Column(name = "received_at") private LocalDateTime receivedAt;
    @Column(name = "returned_to_student", nullable = false) private boolean returnedToStudent;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "returned_by") private User returnedBy;
    @Column(name = "returned_at") private LocalDateTime returnedAt;
    @Column(name = "return_remarks", length = 500) private String returnRemarks;
    public Long getId(){return id;} public AdmissionForm getAdmissionForm(){return admissionForm;}
    public void setAdmissionForm(AdmissionForm v){admissionForm=v;} public String getDocumentType(){return documentType;}
    public void setDocumentType(String v){documentType=v;} public boolean isOriginalReceived(){return originalReceived;}
    public void setOriginalReceived(boolean v){originalReceived=v;} public boolean isXeroxReceived(){return xeroxReceived;}
    public void setXeroxReceived(boolean v){xeroxReceived=v;} public User getReceivedBy(){return receivedBy;}
    public void setReceivedBy(User v){receivedBy=v;} public LocalDateTime getReceivedAt(){return receivedAt;}
    public void setReceivedAt(LocalDateTime v){receivedAt=v;} public boolean isReturnedToStudent(){return returnedToStudent;}
    public void setReturnedToStudent(boolean v){returnedToStudent=v;} public User getReturnedBy(){return returnedBy;}
    public void setReturnedBy(User v){returnedBy=v;} public LocalDateTime getReturnedAt(){return returnedAt;}
    public void setReturnedAt(LocalDateTime v){returnedAt=v;} public String getReturnRemarks(){return returnRemarks;}
    public void setReturnRemarks(String v){returnRemarks=v;}
}
