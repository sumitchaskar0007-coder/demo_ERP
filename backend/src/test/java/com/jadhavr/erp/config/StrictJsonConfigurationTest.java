package com.jadhavr.erp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.assertThrows;

class StrictJsonConfigurationTest {
    private ObjectMapper json;

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new StrictJsonConfiguration().strictJsonCustomizer().customize(builder);
        json = builder.build();
    }

    @Test
    void rejectsUnknownProperties() {
        assertThrows(Exception.class,
                () -> json.readValue("{\"count\":1,\"unexpected\":true}", SampleRequest.class));
    }

    @Test
    void rejectsStringToNumberCoercion() {
        assertThrows(Exception.class,
                () -> json.readValue("{\"count\":\"1\",\"status\":\"ACTIVE\"}", SampleRequest.class));
    }

    @Test
    void rejectsFloatToIntegerCoercion() {
        assertThrows(Exception.class,
                () -> json.readValue("{\"count\":1.5,\"status\":\"ACTIVE\"}", SampleRequest.class));
    }

    @Test
    void rejectsNumericEnums() {
        assertThrows(Exception.class,
                () -> json.readValue("{\"count\":1,\"status\":0}", SampleRequest.class));
    }

    @Test
    void rejectsTrailingJsonContent() {
        assertThrows(Exception.class,
                () -> json.readValue("{\"count\":1,\"status\":\"ACTIVE\"} {}", SampleRequest.class));
    }

    private record SampleRequest(Integer count, Status status) {}

    private enum Status {
        ACTIVE
    }
}
