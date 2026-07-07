-- ============================================================================
-- V2: 기존 UTC 벽시계 데이터 → KST(Asia/Seoul) +9h 이행
--
-- 배경:
--   지금까지 컨테이너 JVM이 UTC로 돌아서 LocalDateTime 컬럼(created_at 등)에
--   UTC 벽시계가 저장됐고, 프론트는 오프셋 없는 ISO 문자열을 KST로 해석해
--   모든 시각이 9시간 과거로 표시됐다. Dockerfile TZ=Asia/Seoul +
--   hikari connection-init-sql(SET TIME ZONE) 적용으로 새 데이터는 KST로
--   기록되므로, 기존 데이터를 +9h 이행해 신/구 데이터를 일치시킨다.
--
-- 안전성:
--   - 신규(빈) DB에서는 전부 0건 UPDATE라 no-op.
--   - 운영 DB에서는 TZ 반영 이미지의 첫 기동 시 Flyway가 1회 실행 —
--     즉 "KST로 쓰기 시작하는 시점"과 "기존 데이터 시프트"가 자동으로 묶인다.
--   - 롤링 배포 중 구 태스크(UTC 기록)가 겹치는 몇 분간의 잔여 행은
--     시프트 대상에서 빠질 수 있음(허용 오차). 완벽히 하려면 배포 시
--     desired-count 0 → 배포 → 복구 순서로.
--
-- 대상 제외:
--   - articles/article_collect_staging 의 publish_date 중 source='NAVER':
--     네이버 pubDate(+0900)는 원래부터 KST 벽시계로 저장돼 있어 시프트 불필요.
--   - article_backups.backup_date(DATE): UTC/KST 어느 기준으로도 같은 날짜.
--   - BATCH_* 메타데이터: Spring Batch 내부 용도.
--   - MongoDB(user_activities): BSON Date(절대시각)라 JVM 시간대 변경 시
--     읽기 시점에 자동으로 올바른 KST가 됨.
-- ============================================================================

UPDATE users SET
    created_at = created_at + interval '9 hours',
    updated_at = updated_at + interval '9 hours',
    deleted_at = deleted_at + interval '9 hours';

UPDATE interests SET
    created_at = created_at + interval '9 hours',
    updated_at = updated_at + interval '9 hours';

UPDATE interest_keywords SET
    created_at = created_at + interval '9 hours';

UPDATE subscriptions SET
    created_at = created_at + interval '9 hours';

UPDATE articles SET
    created_at = created_at + interval '9 hours',
    updated_at = updated_at + interval '9 hours',
    deleted_at = deleted_at + interval '9 hours';

UPDATE articles SET
    publish_date = publish_date + interval '9 hours'
WHERE source <> 'NAVER';

UPDATE article_collect_staging SET
    created_at = created_at + interval '9 hours';

UPDATE article_collect_staging SET
    publish_date = publish_date + interval '9 hours'
WHERE source <> 'NAVER';

UPDATE article_interests SET
    created_at = created_at + interval '9 hours';

UPDATE article_views SET
    created_at = created_at + interval '9 hours';

UPDATE article_backups SET
    created_at = created_at + interval '9 hours';

UPDATE comments SET
    created_at = created_at + interval '9 hours',
    updated_at = updated_at + interval '9 hours',
    deleted_at = deleted_at + interval '9 hours';

UPDATE comment_likes SET
    created_at = created_at + interval '9 hours';

UPDATE notifications SET
    created_at   = created_at + interval '9 hours',
    confirmed_at = confirmed_at + interval '9 hours';
