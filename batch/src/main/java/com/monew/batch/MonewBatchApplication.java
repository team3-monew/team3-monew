package com.monew.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 확인
 */
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class MonewBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonewBatchApplication.class, args);
    }
}
