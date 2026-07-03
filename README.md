# MoNew (모뉴)

![server](.github/badges/jacoco-server.svg) ![batch](.github/badges/jacoco-batch.svg) ![total](.github/badges/jacoco.svg)

> `server` / `batch` 각 모듈 커버리지와 전체(합산) 커버리지. main push 시 자동 갱신.

여러 뉴스 API를 통합해 맞춤형 뉴스를 제공하고, 의견을 나눌 수 있는 소셜 기능을 갖춘 서비스.
**PostgreSQL(원본·트랜잭션) + MongoDB(활동내역 역정규화 조회모델)** 기반, **AWS ECS 자동 배포**.

## 🚀 배포 (Live)

- **서비스 주소**: http://monew-alb-1299817151.ap-northeast-2.elb.amazonaws.com
  - **프론트엔드(React) + REST API가 같은 주소**에서 서빙됩니다 (모노레포 통합).
- **자동 배포(CD)**: `main` push → GitHub Actions가 이미지 빌드 → ECR push → **ECS Fargate 롤링 배포** (GitHub OIDC, AWS 키 미저장).
- **인프라**: AWS **VPC · RDS(PostgreSQL) · ECS Fargate · ALB · S3 · ECR** + **MongoDB Atlas**.
- **로그**: 앱 로그 → CloudWatch → 매일 S3(`monew-app-logs`) 날짜별 적재.

## 📁 프로젝트 구조

**단일 레포 + Gradle 멀티모듈 + 프론트 모노레포.**

```
monew/
├─ frontend/              # React + TypeScript + Vite (server가 정적 서빙)
├─ server/                # API 서버(REST) + 프론트 서빙 → 이미지 1
│   └─ src/main/java/com/monew/server/
│       └─ user/ article/ interest/ comment/ notification/ activity/ subscription/ common/
├─ batch/                 # 배치(수집/백업/복구/정리/알림) → 이미지 2
│   └─ src/main/java/com/monew/batch/
├─ scripts/               # 운영 스크립트 (aws-on/off — 비용 절감 토글, 로컬 전용)
├─ docs/db/schema.sql     # DB 스키마 (원본)
├─ docker-compose.yml     # 로컬 인프라 (PostgreSQL + MongoDB)
└─ .github/workflows/     # CI(빌드·테스트) / CD(배포) / Coverage(커버리지)
```

- **PostgreSQL**: 원본(트랜잭션) 데이터 · **MongoDB**: 활동 내역 등 역정규화 조회 모델(CQRS)

## 🛠 기술 스택

- **백엔드**: Java 17, Spring Boot 3.5.15 (Web / Data JPA / Data MongoDB / Validation / Batch / Actuator), QueryDSL
- **프론트**: React 19 + TypeScript + Vite (모노레포로 server에 포함)
- **DB**: PostgreSQL(RDS) + MongoDB(Atlas)
- **인프라/배포**: AWS(VPC·ECS Fargate·ALB·RDS·S3·ECR), GitHub Actions(OIDC), Docker
- **테스트**: JUnit5 / Mockito, Testcontainers(repository 통합테스트), Jacoco(커버리지)

## 💻 로컬 실행

### 인프라 + 백엔드
```bash
docker compose up -d          # 로컬 PostgreSQL + MongoDB
./gradlew :server:bootRun     # API 서버 (프론트는 빌드 안 함)
./gradlew :batch:bootRun --args='--spring.batch.job.name=articleCollectJob'  # 배치 잡 1회
```


### 프론트엔드 (개발 서버)
```bash
cd frontend && npm install && npm run dev   # localhost:5173, /api 는 :8080 프록시
```
> **Node 22+ 필요** (Vite 7). 없으면 `nvm install 22`.

## 📦 빌드

```bash
./gradlew :server:bootJar     # 서버 + 프론트(dist)가 포함된 실행 jar
./gradlew build               # 전체
```
> `bootJar` 시 Gradle이 Node를 자동으로 받아 프론트를 빌드해 `static/`에 포함합니다.
> `bootRun`/`test` 는 프론트를 빌드하지 않아 백엔드 개발 속도에 영향이 없습니다.

## ✅ 테스트 & 커버리지

```bash
./gradlew test jacocoTestReport
open server/build/reports/jacoco/test/html/index.html   # 패키지·클래스별 커버리지
```
- **Repository 통합테스트**는 `support/RepositoryTestSupport` 상속 → Testcontainers PostgreSQL(+ pg_trgm) 자동 기동. **Docker 실행 필요.**
- **커버리지 배지**는 server / batch / 전체(합산) 3종. 목표 80%.

## 🔄 CI/CD

| 워크플로 | 트리거 | 하는 일 |
|---|---|---|
| **CI/CD** | push/PR | server·batch 빌드·테스트 / `main` push 시 이미지 빌드→ECR→ECS 배포 |
| **Coverage** | push/PR | 커버리지 측정, PR 코멘트, HTML 리포트 아티팩트, 배지 갱신, gh-pages |

## 👥 모듈별 담당

| 담당 | 도메인               |
|---|-------------------|
| 여운정 | 사용자 관리 + 공통 인증·로깅 |
| 오소현 | 관심사               |
| 박서현 | 뉴스 기사 + 수집/백업 배치  |
| 정다운 | 댓글                |
| 이태형 | 활동 내역(MongoDB)    |
| 윤영주 | 알림                |
