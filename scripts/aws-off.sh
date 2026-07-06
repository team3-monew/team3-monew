#!/usr/bin/env bash
# MoNew 비용 절감: 서버+배치(ECS) 0 + RDS 정지. (ALB는 유지)
set -e
REGION=ap-northeast-2
echo "▶ ECS 서버 태스크 0으로..."
aws ecs update-service --region "$REGION" --cluster monew-cluster --service monew-server-svc --desired-count 0 >/dev/null
echo "▶ ECS 배치 태스크 0으로..."
aws ecs update-service --region "$REGION" --cluster monew-cluster --service monew-batch-svc --desired-count 0 >/dev/null 2>&1 \
  && echo "  배치 서비스 0 완료" || echo "  (배치 서비스 없음 — 스킵)"
echo "▶ RDS 정지..."
aws rds stop-db-instance --region "$REGION" --db-instance-identifier monew-db >/dev/null 2>&1 \
  && echo "  RDS 정지 요청 완료" || echo "  (이미 정지 중/정지됨)"
echo "✅ OFF: ECS(서버+배치) 0, RDS 정지. (ALB ~\$18/월은 계속 — 장기면 별도 삭제)"
