- 제목 : fix[#issue번호]: ALB 전용 헬스그룹 분리 (MongoDB 장애 시 전체 서비스 다운 방지)

## ISSUE
[#issueNum] MongoDB(Atlas) 순단 시 서비스 전체가 다운되는 문제 (SPOF)

## 작업 내용

**문제**: server의 `/actuator/health`에 Mongo 헬스체크가 포함되고, ALB 헬스체크가 이 경로에서 200만 통과시킴. Atlas가 순단하면(실제 7/5 16시 UTC Atlas 재기동 이력 있음) health가 503 → ALB가 태스크를 죽임 → 새 태스크도 unhealthy → **활동내역 페이지 하나 때문에 로그인/기사/댓글 전부 다운**되는 구조.

**수정**: `application-prod.yml`에 ALB 전용 헬스그룹 추가

```yaml
management:
  endpoint:
    health:
      group:
        alb:
          include: ping, db   # 프로세스 생존 + PostgreSQL만, Mongo 제외
```

- Atlas 순단 시: `/actuator/health/alb`는 200 유지 → 태스크 생존 → 활동내역 페이지만 에러 (올바른 장애 반경)
- 전체 `/actuator/health`에는 Mongo가 그대로 남아 모니터링 가시성 유지

## 기타 사항

- **배포 후 수동 작업 필수** (이 순서대로, 순서 바뀌면 태스크 즉사):
  1. 배포 완료 후 `curl .../actuator/health/alb` → 200 확인
  2. 타깃그룹 전환:
     ```bash
     aws elbv2 modify-target-group \
       --target-group-arn <monew-server-tg ARN> \
       --health-check-path /actuator/health/alb
     ```
- **롤백 함정**: 타깃그룹 경로 전환 후 이 설정이 없는 옛 이미지로 롤백하면 `/actuator/health/alb`가 404 → unhealthy. 롤백 시 타깃그룹 경로부터 원복할 것.
- 선택 후속 작업(별건): ①SSM `MONGODB_URI`에 `serverSelectionTimeoutMS=5000` 추가(장애 시 30초 블로킹 방지) ②batch의 미사용 mongodb starter 제거(기동 지연/노이즈 제거)
