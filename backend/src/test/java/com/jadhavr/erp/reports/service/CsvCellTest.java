package com.jadhavr.erp.reports.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvCellTest {

    @Test
    void neutralizesFormulaPrefixesEvenAfterWhitespace() {
        assertEquals("\"'=SUM(A1:A2)\"", CsvCell.value("=SUM(A1:A2)"));
        assertEquals("\"'  -10\"", CsvCell.value("  -10"));
        assertEquals("\"'\t@IMPORTXML(x)\"", CsvCell.value("\t@IMPORTXML(x)"));
        assertEquals("\"'+91-555\"", CsvCell.value("+91-555"));
    }

    @Test
    void preservesCsvQuotingAfterNeutralization() {
        assertEquals("\"'=cmd|\"\"payload\"\"\"", CsvCell.value("=cmd|\"payload\""));
        assertEquals("\"ordinary, value\"", CsvCell.value("ordinary, value"));
    }
}
