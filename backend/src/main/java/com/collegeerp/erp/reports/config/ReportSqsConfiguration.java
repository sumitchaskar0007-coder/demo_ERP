package com.collegeerp.erp.reports.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@ConditionalOnExpression(
        "'${app.reports.sqs.producer-enabled:false}' == 'true' || "
                + "'${app.reports.sqs.consumer-enabled:false}' == 'true'")
public class ReportSqsConfiguration {

    @Bean(name = "reportSqsClient")
    SqsClient reportSqsClient(@Value("${app.aws.region}") String region) {
        return SqsClient.builder().region(Region.of(region)).build();
    }
}
