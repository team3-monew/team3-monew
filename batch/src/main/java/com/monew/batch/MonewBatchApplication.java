package com.monew.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 배치 모듈의 Spring Boot 시작점입니다.
 * 스케줄링과 ConfigurationProperties 스캔을 켜서 Spring Batch Job을 주기적으로 실행합니다.
 */
@EnableJpaAuditing
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class MonewBatchApplication {

  public static void main(String[] args) {
    SpringApplication.run(MonewBatchApplication.class, args);
  }
}
