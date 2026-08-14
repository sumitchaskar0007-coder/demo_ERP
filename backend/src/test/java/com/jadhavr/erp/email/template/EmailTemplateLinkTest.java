package com.jadhavr.erp.email.template;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EmailTemplateLinkTest {

    @Test
    void actionUrlsAreNotRenderedAgainAsPlainText() throws IOException, URISyntaxException {
        Path templates = Path.of(getClass().getResource("/templates/email").toURI());

        try (var files = Files.list(templates)) {
            for (Path template : files.filter(path -> path.toString().endsWith(".html")).toList()) {
                String html = Files.readString(template);
                assertFalse(
                        html.contains("th:text=\"${url}\""),
                        () -> template.getFileName() + " renders its action URL more than once");
            }
        }
    }
}
