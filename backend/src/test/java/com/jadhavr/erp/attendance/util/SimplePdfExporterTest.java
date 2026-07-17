package com.jadhavr.erp.attendance.util;
import org.junit.jupiter.api.Test;import java.nio.charset.StandardCharsets;import java.util.List;import static org.assertj.core.api.Assertions.assertThat;
class SimplePdfExporterTest {@Test void createsValidPdfEnvelope(){byte[] pdf=SimplePdfExporter.create(List.of("Attendance","Present: 10"));String value=new String(pdf,StandardCharsets.ISO_8859_1);assertThat(value).startsWith("%PDF-1.4");assertThat(value).contains("Attendance").endsWith("%%EOF");}}
