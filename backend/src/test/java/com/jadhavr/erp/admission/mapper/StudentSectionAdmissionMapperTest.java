package com.jadhavr.erp.admission.mapper;

import com.jadhavr.erp.admission.repository.AdmissionDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class StudentSectionAdmissionMapperTest {

    @Test
    void springInjectsDocumentRepositoryWhenMapperAlsoHasTestConstructor() {
        AdmissionDocumentRepository documents = mock(AdmissionDocumentRepository.class);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AdmissionDocumentRepository.class, () -> documents);
            context.register(StudentSectionAdmissionMapper.class);
            context.refresh();

            StudentSectionAdmissionMapper mapper = context.getBean(StudentSectionAdmissionMapper.class);

            assertSame(documents, ReflectionTestUtils.getField(mapper, "documents"));
        }
    }
}
