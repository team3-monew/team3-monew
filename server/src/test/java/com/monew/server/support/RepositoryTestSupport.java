package com.monew.server.support;

import com.monew.server.common.config.JpaAuditingConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Repository 통합테스트 공통 베이스 (Testcontainers + 실 PostgreSQL).
 *
 * <p>각 도메인 repository 테스트는 이 클래스를 상속만 하면 됨:
 * <pre>
 *   class InterestRepositoryImplTest extends RepositoryTestSupport {
 *       {@code @Autowired} InterestRepository interestRepository;
 *       {@code @Test} void ... { }
 *   }
 * </pre>
 *
 * <p>제공하는 것:
 * <ul>
 *   <li>{@code @DataJpaTest} 슬라이스 + 임베디드(H2) 교체 금지(replace=NONE)</li>
 *   <li>싱글톤 PostgreSQL 컨테이너(전체 테스트에서 1회만 기동해 공유 → 빠름)</li>
 *   <li>pg_trgm 확장 설치(testcontainers/init.sql) — 관심사 유사도 등 Postgres 전용 기능 재현</li>
 *   <li>QueryDSL {@link JPAQueryFactory} 빈</li>
 * </ul>
 *
 * <p>전제: 로컬/CI에 <b>Docker 실행</b> 필요(우리는 이미 docker compose 사용). 별도 DB 셋업 불필요.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// QueryDSL 빈 + JPA Auditing(created_at/updated_at 자동 채움) — @DataJpaTest 기본엔 Auditing이 없어서 필요
@Import({RepositoryTestSupport.QuerydslTestConfig.class, JpaAuditingConfig.class})
public abstract class RepositoryTestSupport {

    // 싱글톤 컨테이너: JVM 전체에서 한 번만 기동해 모든 repository 테스트가 공유
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withInitScript("testcontainers/init.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // 엔티티 기준으로 테이블 생성(항상 최신 엔티티와 일치). pg_trgm 확장은 initScript로 이미 준비됨.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @TestConfiguration
    static class QuerydslTestConfig {
        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }
}
