# BE-auth-service

## 📋 프로젝트 개요
Spring Boot 3.5.5 기반의 인증 서비스로, JWT 토큰 기반 인증과 이메일 인증 기능을 제공합니다.

## 🚀 기술 스택
- **Backend**: Spring Boot 3.5.5, Java 17
- **Database**: JPA/Hibernate
- **Security**: Spring Security, JWT
- **Email**: Spring Mail
- **Cache**: Redis
- **Container**: Docker
- **Orchestration**: Kubernetes

## 🏗️ DevOps 파이프라인

### CI/CD 워크플로우
- **브랜치**: `main`, `develop`
- **트리거**: Push, Pull Request, Manual Dispatch
- **빌드 도구**: Gradle
- **테스트**: JUnit 5, Spring Boot Test

### Docker 이미지 빌드
- **develop 브랜치**: `auth-service:develop` + `{commit-sha}`
- **main 브랜치**: `auth-service:latest` + `{commit-sha}`
- **레지스트리**: Docker Hub

### 자동 배포 프로세스
1. **코드 커밋/merge** → GitHub Actions 트리거
2. **테스트 실행** → Gradle build & test
3. **Docker 이미지 빌드** → SHA 태그로 이미지 생성
4. **인프라 레포 업데이트** → Kubernetes 매니페스트 자동 수정
5. **ArgoCD 배포** → 자동으로 새 버전 배포

### 인프라 관리
- **매니페스트 위치**: `KE-WhyNot/INFRA/k8s/auth-service/`
- **배포 도구**: ArgoCD
- **자동 업데이트**: main 브랜치 merge 시 자동으로 이미지 태그 업데이트

## 🔧 로컬 개발 환경 설정

### 필수 요구사항
- Java 17+
- Gradle 8+
- Docker
- Redis

### 실행 방법
```bash
# 의존성 설치
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# Docker 이미지 빌드
docker build -t auth-service .

# Docker 컨테이너 실행
docker run -p 8080:8080 auth-service
```

## 📁 프로젝트 구조
```
src/
├── main/java/com/youthfi/auth/
│   ├── domain/auth/          # 인증 도메인
│   ├── domain/email/         # 이메일 도메인
│   └── global/               # 공통 설정
├── main/resources/
│   └── application.yml       # 애플리케이션 설정
└── test/                     # 테스트 코드
```

## 🔐 보안 기능
- JWT 토큰 기반 인증
- Refresh Token 관리
- 토큰 블랙리스트/화이트리스트
- 이메일 인증
- CORS 설정

## 📊 모니터링
- Spring Boot Actuator
- Health Check 엔드포인트
- Kubernetes Liveness/Readiness Probe

## 🚀 배포 정보

- **네임스페이스**: default

- **포트**: 8080

- **리소스**: CPU 250m-500m, Memory 256Mi-512Mi

- **헬스체크**: `/actuator/health`