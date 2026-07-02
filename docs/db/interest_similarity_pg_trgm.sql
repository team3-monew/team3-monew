-- 관심사 유사 이름 검색 성능 개선용 SQL
-- 이미 존재하는 로컬 DB 또는 배포 RDS DB에 1회 실행합니다.
-- 실행 목적:
-- 1. normalized_name 컬럼 추가
-- 2. 기존 관심사 데이터의 normalized_name 보정
-- 3. PostgreSQL pg_trgm extension 활성화
-- 4. normalized_name 기반 GIN index 생성

CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE interests
    ADD COLUMN IF NOT EXISTS normalized_name VARCHAR(50);

UPDATE interests
SET normalized_name = lower(regexp_replace(name, '[[:space:]]+', '', 'g'))
WHERE normalized_name IS NULL;

ALTER TABLE interests
    ALTER COLUMN normalized_name SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_interests_normalized_name_trgm
    ON interests
        USING gin (normalized_name gin_trgm_ops);