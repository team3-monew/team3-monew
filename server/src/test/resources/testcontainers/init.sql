-- Repository 통합테스트용 PostgreSQL 컨테이너 초기화
-- 관심사 유사도 검색 등 Postgres 전용 기능(pg_trgm)을 테스트에서도 재현하기 위해 확장 설치
CREATE EXTENSION IF NOT EXISTS pg_trgm;
