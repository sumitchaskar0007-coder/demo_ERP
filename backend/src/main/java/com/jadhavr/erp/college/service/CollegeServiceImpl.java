package com.jadhavr.erp.college.service;

import com.jadhavr.erp.college.dto.CollegeResponse;
import com.jadhavr.erp.college.dto.CreateCollegeRequest;
import com.jadhavr.erp.college.dto.UpdateCollegeRequest;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.college.entity.CollegeStatus;
import com.jadhavr.erp.college.repository.CollegeRepository;
import com.jadhavr.erp.common.exception.DuplicateResourceException;
import com.jadhavr.erp.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.jadhavr.erp.common.dto.PageResponse;
import com.jadhavr.erp.common.exception.BadRequestException;

@Service
@Transactional(readOnly = true)
public class CollegeServiceImpl implements CollegeService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "code", "city", "state", "status", "createdAt", "updatedAt");
    private final CollegeRepository collegeRepository;
    private final CollegeImageStorageService imageStorage;
    public CollegeServiceImpl(CollegeRepository collegeRepository,
            CollegeImageStorageService imageStorage) {
        this.collegeRepository = collegeRepository;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional
    public CollegeResponse createCollege(CreateCollegeRequest request) {
        String normalizedCode = normalizeCode(request.code());
        if (collegeRepository.existsByCode(normalizedCode)) {
            throw new DuplicateResourceException("College already exists with code: " + normalizedCode);
        }

        College college = new College();
        college.setCode(normalizedCode);
        college.setStatus(CollegeStatus.ACTIVE);
        applyCreateFields(college, request);
        College saved = collegeRepository.saveAndFlush(college);
        saved.setLogoUrl(imageStorage.claim(request.logoUrl(), saved.getId(), "logo", null));
        saved.setQrCodeUrl(imageStorage.claim(request.qrCodeUrl(), saved.getId(), "qr-code", null));
        return toResponse(collegeRepository.saveAndFlush(saved));
    }

    @Override
    public List<CollegeResponse> getAllColleges() {
        return collegeRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CollegeResponse getCollegeById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    public CollegeResponse getCollegeByCode(String code) {
        String normalizedCode = normalizeCode(code);
        College college = collegeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "College not found with code: " + normalizedCode));
        return toResponse(college);
    }

    @Override
    @Transactional
    public CollegeResponse updateCollege(Long id, UpdateCollegeRequest request) {
        College college = findById(id);
        college.setName(request.name().trim());
        college.setAddress(request.address());
        college.setCity(request.city());
        college.setState(request.state());
        college.setPincode(request.pincode());
        college.setContactEmail(request.contactEmail());
        college.setContactPhone(request.contactPhone());
        college.setLogoUrl(imageStorage.claim(request.logoUrl(), college.getId(),
                "logo", college.getLogoUrl()));
        college.setQrCodeUrl(imageStorage.claim(request.qrCodeUrl(), college.getId(),
                "qr-code", college.getQrCodeUrl()));
        return toResponse(collegeRepository.saveAndFlush(college));
    }

    @Override
    @Transactional
    public CollegeResponse activateCollege(Long id) {
        College college = findById(id);
        college.setStatus(CollegeStatus.ACTIVE);
        return toResponse(collegeRepository.save(college));
    }

    @Override
    @Transactional
    public CollegeResponse deactivateCollege(Long id) {
        College college = findById(id);
        college.setStatus(CollegeStatus.INACTIVE);
        return toResponse(collegeRepository.save(college));
    }

    @Override
    @Cacheable(cacheNames = "activeColleges", key = "'all'", sync = true)
    public List<CollegeResponse> getActiveColleges() {
        return collegeRepository.findByStatus(CollegeStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PageResponse<CollegeResponse> searchColleges(
            String keyword,
            CollegeStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Unsupported sort field: " + sortBy);
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Sort direction must be asc or desc");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Specification<College> specification = buildSearchSpecification(keyword, status);
        Page<CollegeResponse> result = collegeRepository.findAll(specification, pageable)
                .map(this::toResponse);
        return PageResponse.from(result);
    }

    private Specification<College> buildSearchSpecification(String keyword, CollegeStatus status) {
        Specification<College> specification = Specification.where(null);
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("code")), pattern),
                    builder.like(builder.lower(root.get("city")), pattern),
                    builder.like(builder.lower(root.get("state")), pattern),
                    builder.like(builder.lower(root.get("contactEmail")), pattern)
            ));
        }
        if (status != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.equal(root.get("status"), status));
        }
        return specification;
    }

    private College findById(Long id) {
        return collegeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("College not found with id: " + id));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void applyCreateFields(College college, CreateCollegeRequest request) {
        college.setName(request.name().trim());
        college.setAddress(request.address());
        college.setCity(request.city());
        college.setState(request.state());
        college.setPincode(request.pincode());
        college.setContactEmail(request.contactEmail());
        college.setContactPhone(request.contactPhone());
    }

    private CollegeResponse toResponse(College college) {
        return new CollegeResponse(
                college.getId(),
                college.getName(),
                college.getCode(),
                college.getAddress(),
                college.getCity(),
                college.getState(),
                college.getPincode(),
                college.getContactEmail(),
                college.getContactPhone(),
                college.getLogoUrl() == null ? null : imageStorage.responseUrl(college.getId(), "logo"),
                college.getQrCodeUrl() == null ? null : imageStorage.responseUrl(college.getId(), "qr-code"),
                college.getStatus(),
                college.getCreatedAt(),
                college.getUpdatedAt()
        );
    }
}
