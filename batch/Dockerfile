# 배치 이미지 — CI에서 ./gradlew :batch:bootJar 로 jar 빌드 후 사용
# 빌드 컨텍스트: batch/ 디렉토리
# 실행 시 어떤 잡을 돌릴지 인자로 지정 (예: --spring.batch.job.name=collectJob)
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY build/libs/batch.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
