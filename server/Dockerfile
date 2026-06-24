# 서버(API) 이미지 — CI에서 ./gradlew :server:bootJar 로 jar 빌드 후 사용
# 빌드 컨텍스트: server/ 디렉토리
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY build/libs/server.jar app.jar

EXPOSE 8080

# 운영 프로파일로 실행 (필요 시 ECS에서 SPRING_PROFILES_ACTIVE 등으로 오버라이드)
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
