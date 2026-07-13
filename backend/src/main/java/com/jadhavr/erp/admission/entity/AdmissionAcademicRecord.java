package com.jadhavr.erp.admission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
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
    @Column(name = "marks_percentage", precision = 5, scale = 2)
    private BigDecimal marksPercentage;

    public AdmissionAcademicRecord() {}

    public AdmissionAcademicRecord(String qualification, String instituteName,
            String boardUniversity, String yearOfPassing, BigDecimal marksPercentage) {
        this.qualification = qualification;
        this.instituteName = instituteName;
        this.boardUniversity = boardUniversity;
        this.yearOfPassing = yearOfPassing;
        this.marksPercentage = marksPercentage;
    }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getInstituteName() { return instituteName; }
    public void setInstituteName(String instituteName) { this.instituteName = instituteName; }
    public String getBoardUniversity() { return boardUniversity; }
    public void setBoardUniversity(String boardUniversity) { this.boardUniversity = boardUniversity; }
    public String getYearOfPassing() { return yearOfPassing; }
    public void setYearOfPassing(String yearOfPassing) { this.yearOfPassing = yearOfPassing; }
    public BigDecimal getMarksPercentage() { return marksPercentage; }
    public void setMarksPercentage(BigDecimal marksPercentage) { this.marksPercentage = marksPercentage; }
}
