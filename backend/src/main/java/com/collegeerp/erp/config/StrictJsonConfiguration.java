package com.collegeerp.erp.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Makes the HTTP JSON boundary schema-strict. Bean Validation handles field
 * lengths, ranges, and formats after parsing; this configuration ensures the
 * parser does not first coerce a value into the requested Java type.
 */
@Configuration
public class StrictJsonConfiguration {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictJsonCustomizer() {
        return builder -> builder
                .featuresToEnable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                        DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .featuresToDisable(
                        DeserializationFeature.ACCEPT_FLOAT_AS_INT,
                        MapperFeature.ALLOW_COERCION_OF_SCALARS);
    }
}
