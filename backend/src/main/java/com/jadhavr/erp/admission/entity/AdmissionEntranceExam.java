package com.jadhavr.erp.admission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AdmissionEntranceExam {
    @Column(name = "exam_name", nullable = false, length = 120)
    private String examName;

    @Column(name = "result", nullable = false, length = 100)
    private String result;

    public AdmissionEntranceExam() {}

    public AdmissionEntranceExam(String examName, String result) {
        this.examName = examName;
        this.result = result;
    }

    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
