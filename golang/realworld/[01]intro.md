# [Golang] Go, Echo, GORM으로 RealWorld Backend 구현하기(1) - 소개

> 전체 소스 코드는 [Github](https://github.com/zacscoding/echo-gorm-realworld-app)에 있습니다 :)

1년 전부터 고언어로 전환하면서 나름대로 어떻게 하면 더 좋은? 고언어스러운? 코드로 구현할 수 있을지 많이 고민했습니다.(물론 지금도..)  

다른 많은 프로젝트를 꾸준히 참조하고 있지만, 아무래도 직접 만들 때 가장 와닿아서 Golang 기반으로 예제 애플리케이션을 만들고자 했습니다.  

그러던중 이전에 봤던 [RealWorld](https://github.com/gothinkster/realworld)라는 프로젝트가 생각났고, 해당 스펙을 고언어로 구현 후 포스팅을 하게 되었습니다.
(해당 프로젝트로 PR은 하지 않았습니다.)

[RealWorld](https://github.com/gothinkster/realworld) 프로젝트는 정의된 스펙이 있고 해당 스펙에 맞춰서 Frontend/Backend/Fullstack 별로 아래와 같이 구현 리스트가 있습니다.  

![realworld implements](https://user-images.githubusercontent.com/25560203/128357901-cc715c64-f61d-4f9d-8c85-7863f6f13d09.png)  
(https://codebase.show/projects/realworld)  

해당 Spec은 간단하게 User/Article 2개의 도메인이 있고 사용자간 Follow 할 수 있고 Article 관리, 좋아요 등의 [API Spec](https://github.com/gothinkster/realworld/tree/master/api)이 있습니다.  

---  

# 프로젝트 소개

해당 프로젝트는 아래와 같은 아키텍처를 가지고 있습니다.(정말 간단합니다..!)

![Simple Architecture](https://user-images.githubusercontent.com/25560203/128342030-bfeafe65-cf90-4856-90ef-65e345645d39.png)  
(Redis Cache 관련 부분은 추후에 추가될 예정입니다.)

또한 아래와 같은 기술 스택을 사용합니다.

| 항목                      | 스택                                                                      | 설명                                                                           |
|--------------------------|--------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| Server Code              | **golang**                                                               | 고언어 1.14 버전을 사용합니다.                                                           |
| Database                 | **MySQL**                                                                | 8.0.17의 도커 이미지를 사용합니다.                                                        |
| Migrate                  | **[golang-migrate/migrate](github.com/golang-migrate/migrate)**          | 해당 라이브러리와 SQL문을 이용하여 스키마의 버전을 관리합니다.                                          |
| ORM                      | **[go-gorm/gorm v2](https://github.com/go-gorm/gorm)**                   | GORM v2를 사용합니다.                                                               |
| Logging                  | **[uber-go/zap](https://github.com/uber-go/zap)**                        | 로깅은 zap logger를 사용합니다.                                                        |
| Unit tests               | - **go test**<br/>- **[stretchr/testify](https://github.com/stretchr/testify)** <br/>**- [ory/dockertest](https://github.com/ory/dockertest)**| - `go test`로 테스트를 진행하고 <br/>- `testify`의 `Suite`, `Mock`, `Assert` 등을 이용합니다.<br/>- `dockertest`를 이용하여 테스트 전 MySQL 도커를 띄우고 DB Layer테스트를 진행합니다.          |
| Integration Testing      | **[newman](https://github.com/postmanlabs/newman)**                      | RealWorld에 작성된 Postman 스크립트로 테스트를 진행합니다. <br />(Golang 기반의 e2e test는 아직 구현X) |
| Configuration management | **[knadh/koanf](github.com/knadh/koanf)**                                | `koanf`를 이용하여 default -> env ->  config file 순으로 설정 값을 초기화합니다.        |

프로젝트 구조는 아래와 같습니다.  

- Common  

```shell
$ tree .
.
├── config                      <-- config 관련 패키지
├── api
│   └── types                   <-- api 응답 구조체 정의 패키지                 
├── database                    <-- 데이터 베이스 관련 패키지(Transaction, Util 등)
├── docs                        <-- API 문서 관련 폴더(Swagger, Redoc)
├── integration                 <-- 통합 테스트 관련 폴더
├── logging                     <-- 로그 관련 패키지
├── migrations                  <-- DB Schema 관련 SQL 폴더
├── server                      <-- http server 관련 패키지
├── serverenv                   <-- application environment 관련 패키지
├── main.go                     <-- main.go
├── Dockerfile                  <-- Docker 빌드를 위한 파일
├── Makefile                    <-- CMD를 위한 Makefile
└── utils               
    ├── authutils               <-- 인증 관련 유틸 패키지
    ├── hashutils               <-- 해시 관련 유틸 패키지(비밀번호 암호화)
    └── httputils               <-- http server 관련 유틸 패키지
```


- Article

```shell
$ tree .
.
├── article
│   ├── database                <-- Article DB 관련 패키지(repository)
│   │   ├── fixtures            <-- 테스트 데이터
│   │   └── mocks               <-- Article DB Mock 관련 패키지
│   ├── handler.go              <-- Article 핸들러 관련 파일
│   ├── model                   <-- Article 모델 관련 패키지
│   └── request.go              <-- Article API Request 구조체 관련 파일
```

- User

```shell
$ tree .
.
├── user                        <-- User 관련 최상위 폴더
│   ├── database                <-- User DB 관련 패키지(repository)
│   │   ├── mocks               <-- User DB Mock 관련 패키지
│   ├── model                   <-- User 모델 관련 패키지
│   ├── handler.go              <-- User 핸들러 관련 파일
│   ├── request.go              <-- User API Request 구조체 관련 파일
```  

## 프로젝트 실행  

다음으로는 해당 프로젝트를 Clone하여 직접 실행해보겠습니다.  

```shell
$ git clone git@github.com:zacscoding/echo-gorm-realworld-app.git
```  

> Docker-compose를 이용하여 API Server + MySQL 실행하기

`docker-compose.yaml` 파일을 살펴보시면 아래와 같이 2가지 컨테이너를 정의하였습니다.  

```yaml
version: '3.1'

services:
  db:
    image: mysql:8.0.17
    ...
  app-server:
    image: zacscoding/echo-gorm-realworld-app
    # 현재 폴더를 기준으로 ./Dockerfile을 이용하여 도커 이미지를 빌드합니다.
    build:
      context: .
    ...
```  

해당 파일을 가지고 아래와 같이 `make compose.up`을 실행하면 2개의 컨테이너가 실행 되는것을 확인할 수 있습니다..

```shell
$ cd echo-gorm-realworld-app
$ make compose.up
./scripts/compose.sh up
Building app-server
... API 서버 도커 이미지 빌드중...

... MySQL 실행 ...
db            | Initializing database
db            | 2021-08-05T14:19:54.733240Z 0 [Warning] [MY-011070] [Server] 'Disabling symbolic links using --skip-symbolic-links (or equivalent) is the default. Consider not using this option as it' is deprecated and will be removed in a future release.
db            | 2021-08-05T14:19:54.733379Z 0 [System] [MY-013169] [Server] /usr/sbin/mysqld (mysqld 8.0.17) initializing of server in progress as process 32
db            | 2021-08-05T14:19:57.117817Z 5 [Warning] [MY-010453] [Server] root@localhost is created with an empty password ! Please consider switching off the --initialize-insecure option.
db            | 2021-08-05T14:19:58.688323Z 0 [System] [MY-013170] [Server] /usr/sbin/mysqld (mysqld 8.0.17) initializing of server has completed

... API Server 실행 ...
app-server    | 2021/08/05 14:20:08 load config file from /config/config.yaml
app-server    | 2021-08-05T14:20:08.630Z	INFO	echo-gorm-realworld-app/main.go:30	Starting a new application server. configs
app-server    | {
app-server    |     "db.dataSourceName": "root:****@tcp(db)/local_db?charset=utf8\u0026parseTime=True\u0026multiStatements=true",
app-server    |     "db.migrate.dir": "/migrations/",
app-server    |     "db.migrate.enable": true,
app-server    |     "db.pool.maxIdle": 5,
app-server    |     "db.pool.maxLifetime": "86400s",
app-server    |     "db.pool.maxOpen": 50,
app-server    |     "jwt.secret": "****",
app-server    |     "jwt.sessionTimeout": "240h",
app-server    |     "server.docs.enabled": true,
app-server    |     "server.docs.path": "/config/doc.html",
app-server    |     "server.port": 8080,
app-server    |     "server.readTimeout": "5s",
app-server    |     "server.timeout": "5s",
app-server    |     "server.writeTimeout": "10s"
app-server    | }
app-server    | 2021-08-05T14:20:08.630Z	INFO	serverenv/setup.go:12	Setting up application environments.
...
```

<br /> 

> Docker 확인하기

```shell
// 이미지 생성된 것 확인하기
$ docker images
REPOSITORY                           TAG               IMAGE ID       CREATED         SIZE
zacscoding/echo-gorm-realworld-app   latest            6277aa6de50a   4 hours ago     23.8MB

// 도커 프로세스 확인하기
$ docker ps -a
CONTAINER ID   IMAGE                                COMMAND                  CREATED         STATUS         PORTS                                                    NAMES
5358cb993ccb   zacscoding/echo-gorm-realworld-app   "app-server --config…"   6 minutes ago   Up 6 minutes   0.0.0.0:8080->8080/tcp, :::8080->8080/tcp                app-server
dbc288f45fba   mysql:8.0.17                         "docker-entrypoint.s…"   6 minutes ago   Up 6 minutes   33060/tcp, 0.0.0.0:43306->3306/tcp, :::43306->3306/tcp   db
```

위와 같이 API 서버와 MySQL 컨테이너가 정상 동작하는 것을 확인할 수 있습니다.  

다음으로는 CURL을 이용해서 API 응답값을 확인해보겠습니다.(DB에 아무 데이터가 없어서 빈 응답을 반환합니다.)  

```shell
curl -XGET http://localhost:8080/api/articles |  jq .
{
  "articles": [],
  "articlesCount": 0
}
```  

## 통합 테스트 실행  

RealWorld Project에서는 Postman의 테스트 스크립트를 기반으로 `newman`을 이용하여 통합 테스트를 진행할 수 있습니다.  

```shell
$ tree ./integration/postman
./integration/postman
├── Conduit.postman_collection.json   <-- postman test script
└── run-api-tests.sh                  <-- newman을 이용하여 위의 collection 실행
```  

> 통합 테스트 실행햐기  

```shell
$ make it.postman
+++ dirname integration/postman/run-api-tests.sh
++ cd integration/postman
++ pwd

...

❏ Auth
↳ Register
  POST http://localhost:8080/api/users [200 OK, 342B, 126ms]
  ✓  Response contains "user" property
  ✓  User has "email" property
  ✓  User has "username" property
  ✓  User has "bio" property
  ✓  User has "image" property
  ✓  User has "token" property
  
... 테스트 실행 중 ...

┌─────────────────────────┬───────────────────┬──────────────────┐
│                         │          executed │           failed │
├─────────────────────────┼───────────────────┼──────────────────┤
│              iterations │                 1 │                0 │
├─────────────────────────┼───────────────────┼──────────────────┤
│                requests │                32 │                0 │
├─────────────────────────┼───────────────────┼──────────────────┤
│            test-scripts │                48 │                0 │
├─────────────────────────┼───────────────────┼──────────────────┤
│      prerequest-scripts │                18 │                0 │
├─────────────────────────┼───────────────────┼──────────────────┤
│              assertions │               263 │                0 │
├─────────────────────────┴───────────────────┴──────────────────┤
│ total run duration: 17.6s                                      │
├────────────────────────────────────────────────────────────────┤
│ total data received: 5.46KB (approx)                           │
├────────────────────────────────────────────────────────────────┤
│ average response time: 23ms [min: 5ms, max: 126ms, s.d.: 27ms] │
└────────────────────────────────────────────────────────────────┘  
```

마지막 표에서 확인할 수 있듯이 총 17초 정도 소요하였고 32번의 요청이 있던것을 확인할 수 있습니다.  

---  

# 앞으로?  

이번 포스팅에서는 Golang 기반의 API 서버 예제를 왜 구현했고 프로젝트에 대한 설명 및 실행에 대해 다뤄봤는데요,  

앞으로는 아래와 같은 내용으로 포스팅 할 예정입니다 :) 감사합니다.  

- 설계하기
  - API Spec 분석
  - 데이터 베이스 스키마 설계 및 유저 쿼리 작성
  - Article 관련 쿼리 작성
- 공통 구현 (로깅 등)
- DB 관련 설정 및 구현(gorm, migrate, dockertest)
- DB layer 구현하기
  - User 모델 정의 및 Repositroy 계층 구현 및 테스트
  - Article 모델 정의 및 Repository 계층 구현
  - Article Repository 계층 테스트
- Handler layer 구현
  - Echo 관련 구현 (설정 및 인증)
  - User 관련 핸들러 구현 및 테스트
  - Article 관련 핸들러 구현 및 테스트










