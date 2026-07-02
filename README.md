# MoNew

![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

여러 뉴스 API를 통합해 맞춤형 뉴스를 제공하고, 의견을 나눌 수 있는 소셜 기능을 갖춘 서비스.
MongoDB + PostgreSQL 백업/복구 시스템.

## 프로젝트 구조

단일 레포 + 멀티 모듈(Gradle) 구조. `server`와 `batch`는 독립적으로 빌드/배포됩니다.

```
monew/
├─ settings.gradle        # 모듈 include (server, batch)
├─ build.gradle           # 모든 모듈 공통 설정 (Java 17, Lombok, 테스트)
├─ server/                # API 서버 (REST) → 이미지 1
│   ├─ build.gradle
│   ├─ Dockerfile
│   └─ src/main/java/com/monew/server/
│       ├─ user/ article/ interest/ comment/ notification/ activity/ common/
├─ batch/                 # 배치 (수집/백업/복구/정리) → 이미지 2
│   ├─ build.gradle
│   ├─ Dockerfile
│   └─ src/main/java/com/monew/batch/
│       ├─ article/{collect,backup,restore}/ notification/ user/ common/
├─ docker-compose.yml     # 로컬 인프라 (PostgreSQL + MongoDB)
└─ .github/workflows/     # CI (server/batch 각각 빌드·테스트)
```

- **PostgreSQL**: 원본(트랜잭션) 데이터
- **MongoDB**: 활동 내역 등 역정규화된 조회 모델

## 기술 스택

- Java 17, Spring Boot 3.5.15
- Spring Web / Data JPA / Data MongoDB / Validation / Batch / Actuator
- PostgreSQL, MongoDB
- Gradle (멀티 모듈), Docker, GitHub Actions, AWS ECS

## 로컬 실행

```bash
# 1) 인프라 기동 (PostgreSQL + MongoDB)
docker compose up -d

# 2) 서버 실행
./gradlew :server:bootRun

# 3) 배치 실행 (예시 — 잡 이름 지정)
./gradlew :batch:bootRun --args='--spring.batch.job.name=collectJob'
```

## 빌드

```bash
./gradlew build                # 전체
./gradlew :server:build        # 서버만
./gradlew :batch:build         # 배치만
```

## 모듈별 담당 (사용자 관리 등 도메인 패키지에서 작업)

각 도메인 패키지(`user`, `article`, ...) 안에 controller/service/repository/entity/dto 를 추가하세요. 
