# Multi-stage build를 사용한 Spring Boot 애플리케이션 Dockerfile

# 1단계: 빌드 스테이지
FROM eclipse-temurin:17-jdk-slim AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle wrapper와 build.gradle 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐시 최적화)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew build -x test --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
