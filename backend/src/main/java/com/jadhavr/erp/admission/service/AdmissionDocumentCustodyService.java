package com.jadhavr.erp.admission.service;
import com.jadhavr.erp.admission.dto.*;
import com.jadhavr.erp.admission.entity.*;
import com.jadhavr.erp.admission.repository.*;
import com.jadhavr.erp.auth.security.SecurityUtils;
import com.jadhavr.erp.common.exception.*;
import com.jadhavr.erp.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdmissionDocumentCustodyService {
    private final AdmissionFormRepository admissions; private final AdmissionDocumentCustodyRepository custody;
    private final UserRepository users;
    public AdmissionDocumentCustodyService(AdmissionFormRepository a,AdmissionDocumentCustodyRepository c,UserRepository u){
        admissions=a;custody=c;users=u;
    }
    @Transactional
    public void record(Long admissionId,List<VerifyAdmissionRequest.DocumentCustody> rows){
        AdmissionForm admission=scoped(admissionId);
        var user=users.findById(SecurityUtils.getCurrentUserId()).orElseThrow();
        for(var row: rows==null?List.<VerifyAdmissionRequest.DocumentCustody>of():rows){
            if(!row.originalReceived()&&!row.xeroxReceived()) throw new BadRequestException(
                    "Select Original, Xerox, or both for every verified document");
            AdmissionDocumentCustody item=custody.findByAdmissionFormIdAndDocumentType(admissionId,row.documentType())
                    .orElseGet(AdmissionDocumentCustody::new);
            item.setAdmissionForm(admission);item.setDocumentType(row.documentType());
            item.setOriginalReceived(row.originalReceived());item.setXeroxReceived(row.xeroxReceived());
            item.setReceivedBy(user);item.setReceivedAt(LocalDateTime.now());custody.save(item);
        }
    }
    public List<AdmissionDocumentCustodyResponse> list(Long admissionId){scoped(admissionId);return custody
            .findByAdmissionFormIdOrderByDocumentType(admissionId).stream().map(this::response).toList();}
    @Transactional
    public AdmissionDocumentCustodyResponse returned(Long admissionId,String type,ReturnAdmissionDocumentRequest request){
        scoped(admissionId);var item=custody.findByAdmissionFormIdAndDocumentType(admissionId,type)
                .orElseThrow(()->new ResourceNotFoundException("Document custody record not found"));
        item.setReturnedToStudent(request.returnedToStudent());
        item.setReturnedAt(request.returnedToStudent()?LocalDateTime.now():null);
        item.setReturnedBy(request.returnedToStudent()?users.findById(SecurityUtils.getCurrentUserId()).orElseThrow():null);
        item.setReturnRemarks(request.remarks()==null?null:request.remarks().trim());
        return response(custody.save(item));
    }
    private AdmissionForm scoped(Long id){var a=admissions.findById(id).orElseThrow(()->new ResourceNotFoundException("Admission not found"));
        var current=SecurityUtils.requireCurrentUser();if(!SecurityUtils.isSuperAdmin()&&!a.getCollege().getId().equals(current.getCollegeId()))
            throw new AccessDeniedException("Admission is outside your college");return a;}
    private AdmissionDocumentCustodyResponse response(AdmissionDocumentCustody i){return new AdmissionDocumentCustodyResponse(
            i.getDocumentType(),i.isOriginalReceived(),i.isXeroxReceived(),i.getReceivedAt(),
            i.isReturnedToStudent(),i.getReturnedAt(),i.getReturnRemarks());}
}
