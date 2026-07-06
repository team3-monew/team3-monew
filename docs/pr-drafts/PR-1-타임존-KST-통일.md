- 제목 : fix[#issue번호]: 서비스 전체 타임존 KST 통일 (모든 시각 9시간 오차 수정)

## ISSUE
[#issueNum] 모든 시각(댓글/알림/기사 발행일)이 9시간 전으로 표시되는 문제

## 작업 내용

**문제**: 운영 컨테이너 JVM이 UTC로 돌아서 `LocalDateTime` 컬럼(created_at 등)에 UTC 벽시계가 저장되는데, 프론트는 오프셋 없는 ISO 문자열을 브라우저 로컬(KST)로 해석 → 방금 쓴 댓글이 "9시간 전"으로 표시. 추가로 기사 발행일이 네이버=KST, RSS=UTC로 소스마다 달라 정렬/날짜필터가 어긋남.

**수정 (4계층)**:

1. **JVM 시간대 고정** — `server/Dockerfile`, `batch/Dockerfile`에 `ENV TZ=Asia/Seoul`
   - task def가 아닌 이미지에 박아서 로컬/ECS 어디서든 동일
   - 부수효과: batch cron이 KST로 해석됨 (백업 12:30→03:30 KST, 유저정리 13:00→04:00 KST — 주석 의도와 일치)
2. **DB 세션 시간대 고정** — server/batch `application.yml`에 `hikari.connection-init-sql: SET TIME ZONE 'Asia/Seoul'`
   - `comment_likes`/`article_views` insert와 `articles.updated_at` 갱신이 DB의 `CURRENT_TIMESTAMP`를 쓰기 때문에 JVM만 바꾸면 이 5곳은 계속 UTC로 남음
   - RDS 파라미터그룹/ALTER DATABASE 없이 커넥션 단에서 해결 (Flyway 덤프 무오염)
3. **수집기 발행일 KST 고정** — `CollectedArticleDto`에 `STORAGE_ZONE(Asia/Seoul)` 도입
   - 네이버 pubDate(+0900): `withZoneSameInstant(KST)` — 기존 동작 유지하되 존 명시
   - RSS: `systemDefault` → KST 고정 (기존 9시간 오차의 직접 원인)
   - 테스트 헬퍼(`HankyungRssArticleCollectorTest`)도 KST 고정 — UTC 러너인 CI에서 깨지지 않게
4. **기존 데이터 이행** — Flyway `V2__shift_utc_timestamps_to_kst.sql`
   - 기존 UTC 행 전체 +9h (users/interests/subscriptions/articles/comments/comment_likes/article_views/notifications 등)
   - **제외**: 네이버 기사 publish_date(이미 KST), backup_date(DATE), BATCH_* 메타, MongoDB(절대시각 저장이라 자동 정합)
   - 빈 DB에선 no-op이라 신규 로컬 환경 안전

로컬 docker-compose postgres에도 `TZ`/`PGTZ` 추가 (신규 볼륨용).

## 기타 사항

- **머지 순서**: Flyway 초기 설정 PR(#87) 머지 후에 이 PR 머지 (V2가 V1 위에서 실행됨). `server/application.yml`을 양쪽 PR이 수정하므로 충돌 시 flyway 블록 + hikari 블록 둘 다 유지.
- **배포 전제**: #87 배포 전에 server task def에 `FLYWAY_BASELINE_ON_MIGRATE=true` 환경변수 필요 (기존 운영 DB는 스키마가 이미 있어서 baseline 없이는 Flyway가 기동 실패).
- **배포 시 주의**: V2(데이터 시프트)는 새 이미지 첫 기동 시 Flyway가 자동 1회 실행 — "KST 기록 시작"과 "기존 데이터 시프트"가 자동으로 같은 시점에 묶임. 다만 롤링 배포 중 구 태스크(UTC 기록)와 겹치는 몇 분간의 신규 행은 시프트에서 빠질 수 있음. 완벽히 하려면 `desired-count 0 → 배포 → 복구`, 아니면 몇 분치 오차 허용.
- 프론트 코드는 변경 불필요 (KST 벽시계 = 브라우저 해석과 일치).
