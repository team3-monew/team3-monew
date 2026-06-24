package com.monew.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스켈레톤 단계의 기본 빌드 검증용 테스트.
 *
 * 주의: @SpringBootTest 로 전체 컨텍스트를 띄우면 PostgreSQL/MongoDB 연결이 필요해
 * DB 없이는 실패합니다. 실제 통합 테스트는 Testcontainers 등을 붙여
 * 각자 자신의 도메인에 맞게 추가하세요.
 */
class MonewServerApplicationTests {

    @Test
    void sanityCheck() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
