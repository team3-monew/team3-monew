package com.monew.batch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스켈레톤 단계의 기본 빌드 검증용 테스트.
 *
 * 주의: @SpringBootTest 로 전체 컨텍스트를 띄우면 PostgreSQL/MongoDB 연결이 필요합니다.
 * 실제 배치 테스트는 spring-batch-test + Testcontainers 로 각 업무에 맞게 추가하세요.
 */
class MonewBatchApplicationTests {

    @Test
    void sanityCheck() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
