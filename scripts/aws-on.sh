#!/usr/bin/env bash
# MoNew 재가동: RDS 시작 → available 대기 → 서버+배치(ECS) 1.
set -e
REGION=ap-northeast-2
echo "▶ RDS 시작..."
aws rds start-db-instance --region "$REGION" --db-instance-identifier monew-db >/dev/null 2>&1 \
  && echo "  RDS 시작 요청 완료" || echo "  (이미 시작 중/실행 중)"
echo "▶ RDS 'available' 대기 (몇 분 걸림)..."
aws rds wait db-instance-available --region "$REGION" --db-instance-identifier monew-db
echo "▶ ECS 서버 태스크 1로..."
aws ecs update-service --region "$REGION" --cluster monew-cluster --service monew-server-svc --desired-count 1 >/dev/null
echo "▶ ECS 배치 태스크 1로..."
aws ecs update-service --region "$REGION" --cluster monew-cluster --service monew-batch-svc --desired-count 1 >/dev/null 2>&1 \
  && echo "  배치 서비스 1 완료" || echo "  (배치 서비스 없음 — 스킵)"
echo "✅ ON: 1~2분 뒤 http://monew-alb-1299817151.ap-northeast-2.elb.amazonaws.com"
