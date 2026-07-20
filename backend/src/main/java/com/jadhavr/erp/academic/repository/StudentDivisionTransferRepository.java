package com.jadhavr.erp.academic.repository;
import com.jadhavr.erp.academic.entity.StudentDivisionTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StudentDivisionTransferRepository extends JpaRepository<StudentDivisionTransfer,Long>{List<StudentDivisionTransfer> findTop20ByToSectionDepartmentIdOrderByTransferredAtDesc(Long departmentId);}
