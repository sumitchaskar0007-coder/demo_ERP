package com.collegeerp.erp.admission.controller;

import com.collegeerp.erp.admission.service.AdmissionService;
import com.collegeerp.erp.college.entity.College;
import com.collegeerp.erp.college.repository.CollegeRepository;
import com.collegeerp.erp.college.service.CollegeImageStorageService;
import com.collegeerp.erp.fee.entity.FeeStructure;
import com.collegeerp.erp.fee.enums.StudentCategory;
import com.collegeerp.erp.fee.repository.FeeStructureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicAdmissionControllerTest {
    @Mock private AdmissionService admissionService;
    @Mock private CollegeRepository colleges;
    @Mock private CollegeImageStorageService images;
    @Mock private FeeStructureRepository feeStructures;

    @Test
    void categoriesUseCanonicalOrderAndKeepOtherLast() {
        College college = new College();
        college.setId(1L);
        when(colleges.findByCode("ABC001")).thenReturn(Optional.of(college));
        when(feeStructures.findPublicCategoryOptions(1L, 10L)).thenReturn(List.of(
                fee(9L, StudentCategory.OTHER, "Zulu"),
                fee(5L, StudentCategory.SBC, null),
                fee(1L, StudentCategory.OPEN, null),
                fee(8L, StudentCategory.OTHER, "Alpha"),
                fee(7L, StudentCategory.EWS, null),
                fee(2L, StudentCategory.OBC, null),
                fee(6L, StudentCategory.VJNT, null),
                fee(4L, StudentCategory.ST, null),
                fee(3L, StudentCategory.SC, null)));
        var controller = new PublicAdmissionController(
                admissionService, colleges, images, feeStructures);

        var options = controller.categories("abc001", 10L, null, null, null).data();

        assertEquals(
                List.of(
                        StudentCategory.OPEN,
                        StudentCategory.OBC,
                        StudentCategory.SC,
                        StudentCategory.ST,
                        StudentCategory.SBC,
                        StudentCategory.VJNT,
                        StudentCategory.EWS,
                        StudentCategory.OTHER,
                        StudentCategory.OTHER),
                options.stream().map(option -> option.category()).toList());
        assertEquals(
                List.of("Alpha", "Zulu"),
                options.stream()
                        .filter(option -> option.category() == StudentCategory.OTHER)
                        .map(option -> option.customCategoryName())
                        .toList());
    }

    private FeeStructure fee(Long id, StudentCategory category, String customCategoryName) {
        FeeStructure fee = new FeeStructure();
        fee.setId(id);
        fee.setStudentCategory(category);
        fee.setCustomCategoryName(customCategoryName);
        fee.setGender("MALE");
        return fee;
    }
}
