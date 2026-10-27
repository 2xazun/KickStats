# KickStats

축구 리그의 팀·선수·경기 정보와 골·어시스트·경고 등의 기록을 관리하고, 팀 순위표와 선수 개인 순위 같은 통계를 제공하는 리그 기록·통계 관리 시스템입니다.

## 기술 스택
- Java 17, Spring Boot, Gradle
- Spring Data JPA, Thymeleaf
- MySQL 8.0

## 핵심 기능
1. 리그·시즌 관리: 리그/시즌 CRUD, 시즌별 참가 팀 관리
2. 팀·선수 관리: 팀(연고지, 홈 경기장)과 선수(포지션, 국적, 생년월일) 관리, 계약 단위의 소속·등번호·이적 이력
3. 경기 기록 관리: 경기 일정, 출전 선수(선발/교체, 출전 시간), 골(득점자, 도움, 시간, PK), 경고·퇴장
4. 팀 순위표: 승/무/패, 득점, 실점, 득실차, 승점 자동 계산
5. 선수 개인 기록·순위: 득점·도움·공격포인트·경고 순위, 선수별/팀별 기록
6. 통계 분석: 홈·원정 승률, 팀별 평균 득점, 경기장별 경기 수 등

## 실행 방법
```bash
# 1. DB 생성
mysql -u root -p < db/schema.sql

# 2. DB 비밀번호 환경변수 설정 (PowerShell)
$env:DB_PASSWORD="비밀번호"

# 3. 실행 후 http://localhost:8080 접속
./gradlew bootRun
```

## 폴더 구조
```
db/schema.sql          # DB 스키마
src/main/java/...      # 애플리케이션 코드
src/main/resources/    # 설정, 템플릿(templates/), 정적 파일(static/)
```
