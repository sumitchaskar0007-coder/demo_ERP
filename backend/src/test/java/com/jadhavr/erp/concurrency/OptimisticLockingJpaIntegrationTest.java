package com.jadhavr.erp.concurrency;

import com.jadhavr.erp.academic.entity.AcademicClass;
import com.jadhavr.erp.admission.entity.AdmissionAcademicRecord;
import com.jadhavr.erp.admission.entity.AdmissionForm;
import com.jadhavr.erp.admission.enums.AdmissionStatus;
import com.jadhavr.erp.college.entity.College;
import com.jadhavr.erp.department.entity.Department;
import com.jadhavr.erp.fee.entity.FeeStructure;
import com.jadhavr.erp.student.entity.StudentProfile;
import com.jadhavr.erp.user.entity.Role;
import com.jadhavr.erp.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.hibernate.StaleStateException;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimisticLockingJpaIntegrationTest {

    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void createPersistenceUnit() {
        Map<String, Object> settings = Map.of(
                "jakarta.persistence.jdbc.driver", "org.h2.Driver",
                "jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:optimistic_" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "jakarta.persistence.jdbc.user", "sa",
                "jakarta.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false",
                "hibernate.format_sql", "false"
        );
        entityManagerFactory = new HibernatePersistenceProvider()
                .createContainerEntityManagerFactory(new TestPersistenceUnitInfo(), settings);
        assertNotNull(entityManagerFactory);
    }

    @AfterEach
    void closePersistenceUnit() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void overlappingAdmissionWorkflowUpdatesRejectTheStaleTransaction() {
        Fixture fixture = persistFixture();

        assertSecondOverlappingUpdateRejected(
                AdmissionForm.class,
                fixture.admissionId(),
                admission -> admission.setStatus(
                        AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING),
                admission -> admission.setStatus(AdmissionStatus.CANCELLED));

        EntityManager verification = entityManagerFactory.createEntityManager();
        try {
            AdmissionForm admission = verification.find(
                    AdmissionForm.class, fixture.admissionId());
            assertEquals(
                    AdmissionStatus.STUDENT_SECTION_REVIEW_PENDING,
                    admission.getStatus());
            assertEquals(1L, admission.getVersion());
        } finally {
            verification.close();
        }
    }

    @Test
    void overlappingFeeStructureAdminEditsRejectTheStaleTransaction() {
        Fixture fixture = persistFixture();

        assertSecondOverlappingUpdateRejected(
                FeeStructure.class,
                fixture.feeStructureId(),
                feeStructure -> feeStructure.setTitle("First committed edit"),
                feeStructure -> feeStructure.setTitle("Stale overwritten edit"));

        EntityManager verification = entityManagerFactory.createEntityManager();
        try {
            FeeStructure feeStructure = verification.find(
                    FeeStructure.class, fixture.feeStructureId());
            assertEquals("First committed edit", feeStructure.getTitle());
            assertEquals(1L, feeStructure.getVersion());
        } finally {
            verification.close();
        }
    }

    private <T> void assertSecondOverlappingUpdateRejected(
            Class<T> entityType,
            Long id,
            Consumer<T> firstUpdate,
            Consumer<T> staleUpdate) {
        EntityManager firstManager = entityManagerFactory.createEntityManager();
        EntityManager staleManager = entityManagerFactory.createEntityManager();
        try {
            firstManager.getTransaction().begin();
            staleManager.getTransaction().begin();

            T firstCopy = firstManager.find(entityType, id);
            T staleCopy = staleManager.find(entityType, id);
            assertNotNull(firstCopy);
            assertNotNull(staleCopy);

            firstUpdate.accept(firstCopy);
            firstManager.getTransaction().commit();

            staleUpdate.accept(staleCopy);
            RuntimeException conflict = assertThrows(
                    RuntimeException.class,
                    () -> staleManager.getTransaction().commit());
            assertTrue(
                    hasCause(conflict, OptimisticLockException.class)
                            || hasCause(conflict, StaleStateException.class),
                    () -> "Expected an optimistic-lock cause but got " + conflict);
        } finally {
            rollbackIfActive(firstManager);
            rollbackIfActive(staleManager);
            firstManager.close();
            staleManager.close();
        }
    }

    private Fixture persistFixture() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();

            College college = new College();
            college.setName("Optimistic Lock College");
            college.setCode("OLC");
            entityManager.persist(college);

            Department department = new Department();
            department.setCollege(college);
            department.setName("Computer Science");
            department.setCode("CSE");
            entityManager.persist(department);

            User studentUser = new User();
            studentUser.setCollege(college);
            studentUser.setFullName("Concurrent Student");
            studentUser.setEmail("concurrent.student@example.test");
            studentUser.setPasswordHash("not-used-in-this-test");
            entityManager.persist(studentUser);

            StudentProfile student = new StudentProfile();
            student.setUser(studentUser);
            student.setCollege(college);
            student.setDepartment(department);
            student.setAdmissionNumber("ADM-OPT-1");
            student.setFirstName("Concurrent");
            student.setLastName("Student");
            student.setFullName("Concurrent Student");
            student.setEmail("concurrent.student@example.test");
            student.setPhone("9999999999");
            student.setDateOfBirth(LocalDate.of(2005, 1, 1));
            student.setGender("Other");
            student.setParentName("Parent");
            student.setParentPhone("8888888888");
            entityManager.persist(student);

            AdmissionForm admission = new AdmissionForm();
            admission.setAdmissionReferenceNumber("FORM-OPT-1");
            admission.setCollege(college);
            admission.setDepartment(department);
            admission.setStudent(student);
            admission.setStudentUser(studentUser);
            admission.setAcademicYear("2026-27");
            admission.setFirstName("Concurrent");
            admission.setLastName("Student");
            admission.setFullName("Concurrent Student");
            admission.setEmail("concurrent.student@example.test");
            admission.setPhone("9999999999");
            admission.setDateOfBirth(LocalDate.of(2005, 1, 1));
            admission.setGender("Other");
            admission.setParentName("Parent");
            admission.setParentPhone("8888888888");
            admission.setSubmittedAt(LocalDateTime.now());
            entityManager.persist(admission);

            FeeStructure feeStructure = new FeeStructure();
            feeStructure.setCollege(college);
            feeStructure.setDepartment(department);
            feeStructure.setAcademicYear("2026-27");
            feeStructure.setCourseYear("First Year");
            feeStructure.setTitle("Original fee structure");
            feeStructure.setTotalFee(new BigDecimal("100000.00"));
            feeStructure.setMinimumAmountForAdmission(new BigDecimal("25000.00"));
            entityManager.persist(feeStructure);

            entityManager.getTransaction().commit();
            return new Fixture(admission.getId(), feeStructure.getId());
        } finally {
            rollbackIfActive(entityManager);
            entityManager.close();
        }
    }

    private static boolean hasCause(Throwable throwable, Class<?> causeType) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (causeType.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private static void rollbackIfActive(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    private record Fixture(Long admissionId, Long feeStructureId) {}

    private static final class TestPersistenceUnitInfo implements PersistenceUnitInfo {

        private static final List<String> MANAGED_CLASSES = List.of(
                AdmissionForm.class.getName(),
                AdmissionAcademicRecord.class.getName(),
                FeeStructure.class.getName(),
                College.class.getName(),
                Department.class.getName(),
                AcademicClass.class.getName(),
                StudentProfile.class.getName(),
                User.class.getName(),
                Role.class.getName()
        );

        @Override
        public String getPersistenceUnitName() {
            return "optimistic-locking-test";
        }

        @Override
        public String getPersistenceProviderClassName() {
            return HibernatePersistenceProvider.class.getName();
        }

        @Override
        public PersistenceUnitTransactionType getTransactionType() {
            return PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }

        @Override
        public DataSource getJtaDataSource() {
            return null;
        }

        @Override
        public DataSource getNonJtaDataSource() {
            return null;
        }

        @Override
        public List<String> getMappingFileNames() {
            return List.of();
        }

        @Override
        public List<URL> getJarFileUrls() {
            return List.of();
        }

        @Override
        public URL getPersistenceUnitRootUrl() {
            return null;
        }

        @Override
        public List<String> getManagedClassNames() {
            return MANAGED_CLASSES;
        }

        @Override
        public boolean excludeUnlistedClasses() {
            return true;
        }

        @Override
        public SharedCacheMode getSharedCacheMode() {
            return SharedCacheMode.NONE;
        }

        @Override
        public ValidationMode getValidationMode() {
            return ValidationMode.NONE;
        }

        @Override
        public Properties getProperties() {
            return new Properties();
        }

        @Override
        public String getPersistenceXMLSchemaVersion() {
            return "3.1";
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }

        @Override
        public void addTransformer(ClassTransformer transformer) {
            // No test-time bytecode transformation is needed.
        }

        @Override
        public ClassLoader getNewTempClassLoader() {
            return getClassLoader();
        }
    }
}
