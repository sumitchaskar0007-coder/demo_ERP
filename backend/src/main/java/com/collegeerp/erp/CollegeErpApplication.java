package com.collegeerp.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CollegeErpApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CollegeErpApplication.class, args);
        if (context.getEnvironment().getProperty("app.migration-task", Boolean.class, false)) {
            int exitCode = SpringApplication.exit(context);
            System.exit(exitCode);
        }
    }
}
