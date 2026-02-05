# Boilerplate - Spring Boot

## 구성 (Services)
    - 기능(Feature)
        - 게시판 CRUD
        - 로그인 / 인증
        - OAuth 2.0 인증
    - 통합(Global)
        - log4js 설정
        - 어노테이션 추가
            - @LogExecutionTime: 메소드에 추가 시 로그에 해당 메소드 실행 시간 출력 (디버깅용)
## application.yml 설정 파일 등록
    - 로컬 환경
        local.env 파일 생성
``` yml
예시
DB_HOST=localhost
DB_PORT=5432
DB_DATABASE=demo
DB_USER=postgres
DB_PASSWORD=qwe123
JWT_SECRET_KEY=123fewfoijc091109e2i109dwuhqwiudj1kldio1209du09uaioascjoiasu012ud8013fvdsc098u90csd
```

    - 배포 환경
        배포 환경 내 시크릿 키로 local.env 파일 내 변수들 키 등록 필요