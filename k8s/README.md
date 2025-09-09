# Auth 서비스 쿠버네티스 배포 가이드

## 1. 인프라 요구사항

### A. 쿠버네티스 클러스터
- 쿠버네티스 클러스터가 실행 중이어야 합니다.
- 인프라 레포에서 NGINX Ingress Controller가 설치되어 있어야 합니다.

### B. ArgoCD (선택사항)
- ArgoCD가 설치되어 있으면 자동 배포가 가능합니다.
- 인프라 레포에서 ArgoCD Application을 관리합니다.

## 2. Auth 서비스 배포

### A. ArgoCD를 통한 자동 배포 (권장)
```bash
# 인프라 레포에서 ArgoCD Application 생성
# 이 레포의 k8s/ 디렉토리를 참조합니다.
```

### B. 수동 배포 (개발/테스트용)
```bash
# 1. 서비스 배포
kubectl apply -f k8s/services.yaml

# 2. 애플리케이션 배포
kubectl apply -f k8s/deploy.yaml
```

## 3. 실행 확인

```bash
# Pod 상태 확인
kubectl get pods

# 서비스 상태 확인
kubectl get services

# Ingress 상태 확인
kubectl get ingress

# NGINX Ingress Controller 로그 확인
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

## 4. 접속 테스트

```bash
# 로컬에서 접속 테스트
curl http://localhost/api/auth/health

# JWT 토큰으로 인증 테스트
curl -H "Authorization: Bearer <JWT_TOKEN>" http://localhost/api/finance/assets
```

## 5. 트러블슈팅

### NGINX Ingress Controller가 실행되지 않는 경우
```bash
# 네임스페이스 확인
kubectl get namespaces

# Pod 상태 확인
kubectl describe pods -n ingress-nginx

# 이벤트 확인
kubectl get events -n ingress-nginx
```

### 서비스가 연결되지 않는 경우
```bash
# 서비스 엔드포인트 확인
kubectl get endpoints

# Pod 로그 확인
kubectl logs <pod-name>

# 서비스 디버깅
kubectl describe service <service-name>
```

## 6. 개발 환경 설정

### Minikube 사용 시
```bash
# Minikube 시작
minikube start

# NGINX Ingress Controller 활성화
minikube addons enable ingress

# 접속 URL 확인
minikube service list
```

### Docker Desktop Kubernetes 사용 시
```bash
# Kubernetes 활성화 확인
kubectl cluster-info

# NGINX Ingress Controller 설치
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
```

## 7. Auth 서비스 특화 설정

### JWT 토큰 설정
- `application.yml`에서 JWT 관련 설정 관리
- NGINX Ingress Controller와 연동을 위한 내부 API 제공

### 내부 API 엔드포인트
- `/api/auth/internal/validate-token`: JWT 토큰 검증
- `/api/auth/internal/login`: 로그인 페이지 리다이렉트
- `/api/auth/internal/user-info/{userId}`: 사용자 정보 조회

### 보안 고려사항
- JWT 토큰 검증 및 사용자 정보 추출
- CORS 설정으로 프론트엔드와 연동
- 내부 API는 클러스터 내에서만 접근 가능

### 모니터링
- Spring Boot Actuator 엔드포인트 제공
- JWT 토큰 검증 로그 및 메트릭 수집
