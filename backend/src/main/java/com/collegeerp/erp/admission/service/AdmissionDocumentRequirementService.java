package com.collegeerp.erp.admission.service;

import com.collegeerp.erp.admission.dto.AdmissionDocumentRequirementRequest;
import com.collegeerp.erp.admission.dto.AdmissionDocumentRequirementResponse;
import com.collegeerp.erp.admission.entity.AdmissionDocumentRequirement;
import com.collegeerp.erp.admission.entity.AdmissionForm;
import com.collegeerp.erp.admission.repository.AdmissionDocumentRequirementRepository;
import com.collegeerp.erp.admission.repository.AdmissionFormRepository;
import com.collegeerp.erp.auth.security.SecurityUtils;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.common.exception.BadRequestException;
import com.collegeerp.erp.common.exception.DuplicateResourceException;
import com.collegeerp.erp.common.exception.ResourceNotFoundException;
import com.collegeerp.erp.department.entity.Department;
import com.collegeerp.erp.department.repository.DepartmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AdmissionDocumentRequirementService {
    private static final List<DefaultRequirement> DEFAULTS = List.of(
            new DefaultRequirement("TENTH_MARKSHEET", "10th Marksheet", true),
            new DefaultRequirement("TWELFTH_MARKSHEET", "12th Marksheet", true),
            new DefaultRequirement("PROVISIONAL_CERTIFICATE", "Provisional Certificate", true),
            new DefaultRequirement("TRANSFER_CERTIFICATE", "Transfer Certificate", true),
            new DefaultRequirement("NATIONALITY_CERTIFICATE", "Nationality Certificate", true),
            new DefaultRequirement("DOMICILE_CERTIFICATE", "Domicile Certificate", true),
            new DefaultRequirement("AADHAAR_CARD", "Aadhaar Card", true),
            new DefaultRequirement("GRADUATION_MARKSHEET", "Graduation Marksheet", false),
            new DefaultRequirement("MIGRATION_CERTIFICATE", "Migration Certificate", false),
            new DefaultRequirement("GAP_CERTIFICATE", "Gap Certificate", false),
            new DefaultRequirement("ENTRANCE_SCORE_CARD", "Entrance Score Card", false),
            new DefaultRequirement("CASTE_CERTIFICATE", "Caste Certificate", false),
            new DefaultRequirement("CASTE_VALIDITY", "Caste Validity", false),
            new DefaultRequirement("NON_CREAMY_LAYER_CERTIFICATE",
                    "Non-Creamy Layer Certificate", false),
            new DefaultRequirement("NAME_CHANGE_CERTIFICATE", "Name Change Certificate", false),
            new DefaultRequirement("INCOME_CERTIFICATE", "Income Certificate", false),
            new DefaultRequirement("FORM_O_MINORITY", "Form O / Minority Certificate", false));
    private final AdmissionDocumentRequirementRepository requirements;
    private final CollegeRepository colleges;
    private final DepartmentRepository departments;
    private final AdmissionFormRepository admissions;

    public AdmissionDocumentRequirementService(
            AdmissionDocumentRequirementRepository requirements,
            CollegeRepository colleges,
            DepartmentRepository departments,
            AdmissionFormRepository admissions) {
        this.requirements = requirements;
        this.colleges = colleges;
        this.departments = departments;
        this.admissions = admissions;
    }

    public List<AdmissionDocumentRequirementResponse> settings(
            Long requestedCollegeId, Long departmentId) {
        Department department = managedDepartment(requestedCollegeId, departmentId);
        seedDefaults(department);
        return requirements.findByDepartmentIdOrderByDisplayOrderAscIdAsc(department.getId())
                .stream().map(this::response).toList();
    }

    public List<AdmissionDocumentRequirementResponse> activeForDepartment(Long departmentId) {
        Department department = departments.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        seedDefaults(department);
        return requirements.findByDepartmentIdAndActiveTrueOrderByDisplayOrderAscIdAsc(departmentId)
                .stream().map(this::response).toList();
    }

    public List<AdmissionDocumentRequirementResponse> activeForMine() {
        AdmissionForm admission = admissions
                .findTopByStudentUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        return activeForDepartment(admission.getDepartment().getId());
    }

    public List<AdmissionDocumentRequirementResponse> activeForAdmission(Long admissionId) {
        AdmissionForm admission = admissions.findById(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found"));
        if (SecurityUtils.hasRole("STUDENT")) {
            if (!admission.getStudentUser().getId().equals(SecurityUtils.getCurrentUserId())) {
                throw new AccessDeniedException("Admission does not belong to the current student");
            }
        } else if (!SecurityUtils.isSuperAdmin()
                && !admission.getCollege().getId().equals(
                        SecurityUtils.requireCurrentUser().getCollegeId())) {
            throw new AccessDeniedException("Admission is outside your college");
        }
        return activeForDepartment(admission.getDepartment().getId());
    }

    @Transactional
    public AdmissionDocumentRequirementResponse create(
            Long requestedCollegeId, Long departmentId,
            AdmissionDocumentRequirementRequest request) {
        Department department = managedDepartment(requestedCollegeId, departmentId);
        String name = normalizeName(request.documentName());
        if (requirements.existsByDepartmentIdAndDocumentNameIgnoreCase(department.getId(), name)) {
            throw new DuplicateResourceException("Admission document name already exists");
        }
        AdmissionDocumentRequirement requirement = new AdmissionDocumentRequirement();
        requirement.setCollege(department.getCollege());
        requirement.setDepartment(department);
        requirement.setDocumentKey("CUSTOM_" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 24).toUpperCase(Locale.ROOT));
        requirement.setDocumentName(name);
        requirement.setRequired(request.required());
        requirement.setActive(true);
        int nextOrder = requirements.findByDepartmentIdOrderByDisplayOrderAscIdAsc(department.getId())
                .stream().mapToInt(AdmissionDocumentRequirement::getDisplayOrder)
                .max().orElse(0) + 10;
        requirement.setDisplayOrder(nextOrder);
        return response(requirements.save(requirement));
    }

    @Transactional
    public AdmissionDocumentRequirementResponse update(
            Long id, Long requestedCollegeId, Long departmentId,
            AdmissionDocumentRequirementRequest request) {
        Department department = managedDepartment(requestedCollegeId, departmentId);
        AdmissionDocumentRequirement requirement = scoped(id, department.getId());
        String name = normalizeName(request.documentName());
        requirements.findByDepartmentIdOrderByDisplayOrderAscIdAsc(department.getId()).stream()
                .filter(item -> !item.getId().equals(id))
                .filter(item -> item.getDocumentName().equalsIgnoreCase(name))
                .findAny()
                .ifPresent(item -> {
                    throw new DuplicateResourceException("Admission document name already exists");
                });
        requirement.setDocumentName(name);
        requirement.setRequired(request.required());
        return response(requirements.save(requirement));
    }

    @Transactional
    public AdmissionDocumentRequirementResponse setActive(
            Long id, Long requestedCollegeId, Long departmentId, boolean active) {
        Department department = managedDepartment(requestedCollegeId, departmentId);
        AdmissionDocumentRequirement requirement = scoped(id, department.getId());
        requirement.setActive(active);
        return response(requirements.save(requirement));
    }

    public Set<String> requiredKeys(Long departmentId) {
        Department department = departments.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        seedDefaults(department);
        return requirements.findRequiredKeys(departmentId);
    }

    @Transactional
    public void seedDefaults(Department department) {
        if (!requirements.findByDepartmentIdOrderByDisplayOrderAscIdAsc(
                department.getId()).isEmpty()) {
            return;
        }
        for (int index = 0; index < DEFAULTS.size(); index++) {
            DefaultRequirement item = DEFAULTS.get(index);
            AdmissionDocumentRequirement requirement = new AdmissionDocumentRequirement();
            requirement.setCollege(department.getCollege());
            requirement.setDepartment(department);
            requirement.setDocumentKey(item.key());
            requirement.setDocumentName(item.name());
            requirement.setRequired(item.required());
            requirement.setActive(true);
            requirement.setDisplayOrder((index + 1) * 10);
            requirements.save(requirement);
        }
    }

    public void requireActive(AdmissionForm admission, String rawKey) {
        String key = normalizeKey(rawKey);
        seedDefaults(admission.getDepartment());
        requirements.findByDepartmentIdAndDocumentKeyAndActiveTrue(
                        admission.getDepartment().getId(), key)
                .orElseThrow(() -> new BadRequestException(
                        "This admission document is not configured for the department"));
    }

    public static String normalizeKey(String key) {
        if (key == null || !key.matches("[A-Za-z][A-Za-z0-9_]{1,59}")) {
            throw new BadRequestException("Admission document key is invalid");
        }
        return key.toUpperCase(Locale.ROOT);
    }

    private College managedCollege(Long requestedCollegeId) {
        Long collegeId;
        if (SecurityUtils.isSuperAdmin()) {
            if (requestedCollegeId == null) throw new BadRequestException("College is required");
            collegeId = requestedCollegeId;
        } else if (SecurityUtils.isPrincipal()) {
            collegeId = SecurityUtils.requireCurrentUser().getCollegeId();
            if (collegeId == null) throw new BadRequestException("Principal has no college assigned");
            if (requestedCollegeId != null && !requestedCollegeId.equals(collegeId)) {
                throw new AccessDeniedException("Admission document settings are outside your college");
            }
        } else {
            throw new AccessDeniedException("Admission document settings access denied");
        }
        return colleges.findById(collegeId)
                .orElseThrow(() -> new ResourceNotFoundException("College not found"));
    }

    private Department managedDepartment(Long requestedCollegeId, Long departmentId) {
        if (departmentId == null) throw new BadRequestException("Department is required");
        College college = managedCollege(requestedCollegeId);
        Department department = departments.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (!department.getCollege().getId().equals(college.getId())) {
            throw new AccessDeniedException("Department is outside your college");
        }
        return department;
    }

    private AdmissionDocumentRequirement scoped(Long id, Long departmentId) {
        return requirements.findByIdAndDepartmentId(id, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Admission document setting not found"));
    }

    private String normalizeName(String name) {
        if (name == null) throw new BadRequestException("Document name is required");
        String normalized = name.trim().replaceAll("\\s+", " ");
        if (normalized.length() < 2 || normalized.length() > 120) {
            throw new BadRequestException("Document name must contain 2 to 120 characters");
        }
        return normalized;
    }

    private AdmissionDocumentRequirementResponse response(AdmissionDocumentRequirement item) {
        return new AdmissionDocumentRequirementResponse(
                item.getId(), item.getDepartment().getId(), item.getDepartment().getName(),
                item.getDocumentKey(), item.getDocumentName(),
                item.isRequired(), item.isActive(), item.getDisplayOrder());
    }

    private record DefaultRequirement(String key, String name, boolean required) {}
}
