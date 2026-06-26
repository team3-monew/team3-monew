package com.monew.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MonewBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonewBatchApplication.class, args);
    }
}