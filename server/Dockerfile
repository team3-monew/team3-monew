# 서버(API) 이미지 — CI에서 ./gradlew :server:bootJar 로 jar 빌드 후 사용
# 빌드 컨텍스트: server/ 디렉토리
FROM eclipse-temurin:17-jre

# 서비스 기준 시간대 고정 — created_at 등 LocalDateTime이 전부 KST 벽시계로 기록됨
# (프론트가 오프셋 없는 ISO 문자열을 브라우저 로컬(KST)로 해석하므로 반드시 일치해야 함)
ENV TZ=Asia/Seoul

WORKDIR /app
COPY build/libs/server.jar app.jar

EXPOSE 8080

# 운영 프로파일로 실행 (필요 시 ECS에서 SPRING_PROFILES_ACTIVE 등으로 오버라이드)
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
