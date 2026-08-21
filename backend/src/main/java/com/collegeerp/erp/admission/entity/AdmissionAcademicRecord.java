package com.collegeerp.erp.admission.entity;

import com.collegeerp.erp.admission.enums.AcademicGradingType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

@Embeddable
public class AdmissionAcademicRecord {
    @Column(name = "qualification", nullable = false, length = 30)
    private String qualification;
    @Column(name = "institute_name", length = 200)
    private String instituteName;
    @Column(name = "board_university", length = 150)
    private String boardUniversity;
    @Column(name = "year_of_passing", length = 10)
    private String yearOfPassing;
    @Enumerated(EnumType.STRING)
    @Column(name = "grading_type", length = 20)
    private AcademicGradingType gradingType;
    @Column(name = "marks_percentage", precision = 5, scale = 2)
    private BigDecimal marksPercentage;
    @Column(name = "cgpa", precision = 4, scale = 2)
    private BigDecimal cgpa;
    @Column(name = "total_marks", precision = 10, scale = 2)
    private BigDecimal totalMarks;
    @Column(name = "obtained_marks", precision = 10, scale = 2)
    private BigDecimal obtainedMarks;

    public AdmissionAcademicRecord() {}

    public AdmissionAcademicRecord(String qualification, String instituteName,
            String boardUniversity, String yearOfPassing, AcademicGradingType gradingType,
            BigDecimal totalMarks, BigDecimal obtainedMarks, BigDecimal marksPercentage,
            BigDecimal cgpa) {
        this.qualification = qualification;
        this.instituteName = instituteName;
        this.boardUniversity = boardUniversity;
        this.yearOfPassing = yearOfPassing;
        this.gradingType = gradingType;
        this.totalMarks = totalMarks;
        this.obtainedMarks = obtainedMarks;
        this.marksPercentage = marksPercentage;
        this.cgpa = cgpa;
    }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getInstituteName() { return instituteName; }
    public void setInstituteName(String instituteName) { this.instituteName = instituteName; }
    public String getBoardUniversity() { return boardUniversity; }
    public void setBoardUniversity(String boardUniversity) { this.boardUniversity = boardUniversity; }
    public String getYearOfPassing() { return yearOfPassing; }
    public void setYearOfPassing(String yearOfPassing) { this.yearOfPassing = yearOfPassing; }
    public AcademicGradingType getGradingType() { return gradingType; }
    public void setGradingType(AcademicGradingType gradingType) { this.gradingType = gradingType; }
    public BigDecimal getMarksPercentage() { return marksPercentage; }
    public void setMarksPercentage(BigDecimal marksPercentage) { this.marksPercentage = marksPercentage; }
    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }
    public BigDecimal getTotalMarks() { return totalMarks; }
    public void setTotalMarks(BigDecimal totalMarks) { this.totalMarks = totalMarks; }
    public BigDecimal getObtainedMarks() { return obtainedMarks; }
    public void setObtainedMarks(BigDecimal obtainedMarks) { this.obtainedMarks = obtainedMarks; }
}
