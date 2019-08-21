# Zookeeper  

- <a href="#zookeeper">Zookeeper 란?</a>
- <a href="#zknamespace">Zookeeper 데이터 모델</a>
- <a href="#ensemble">Zookeeper 서버 구성도</a>
- <a href="#usage">Basic usage</a>
- <a href="#apply">Zookeeper 활용</a>
- <a href="#ref">Reference</a>

---  

<div id="zookeeper"></div>

## Zookeepr 란?    

- 분산 코디네이션 서비스를 위한 오픈소스 프로젝트
- 분산 처리 환경에서 사용 가능 한 데이터 저장소  
- 활용 분야
  - 서버 간의 정보 공유 (설정 관리)
  - 서버 투입|제거 시 이벤트 처리(클러스터 관리)
  - 리더 선출
  - 서버 모니터링
  - 분산 락 처리
  - 장애 상황 판단

---  

<div id="zknamespace"></div>  

## Zookeeper 데이터 모델  

![zookeeper namespace](./pics/zknamespace.jpg)  

- 일반적인 파일 시스템과 비슷
- 각각의 디렉터리 노드를 znode라고 명명(변경X, 스페이스X)  
- 노드는 영구(Persistent) / 임시(Ephemeral) 노드가 존재  
  - Persistent 노드는 세션 종료 시 삭제되지 않고 데이터가 유지(명시적으로 삭제)  
  - Ephemeral 노드는 세션이 유효한 동안 그 노드의 데이터가 유효  
  - Sequential 은 노드 생성 시 시퀀스 넘버가 자동으로 append

> org.apache.zookeeper.CreateMode.java  

```
package org.apache.zookeeper;

...
@InterfaceAudience.Public
public enum CreateMode {

  /**
   * The znode will not be automatically deleted upon client's disconnect.
   */
  PERSISTENT (0, false, false),
  /**
  * The znode will not be automatically deleted upon client's disconnect,
  * and its name will be appended with a monotonically increasing number.
  */
  PERSISTENT_SEQUENTIAL (2, false, true),
  /**
   * The znode will be deleted upon the client's disconnect.
   */
  EPHEMERAL (1, true, false),
  /**
   * The znode will be deleted upon the client's disconnect, and its name
   * will be appended with a monotonically increasing number.
   */
  EPHEMERAL_SEQUENTIAL (3, true, true);

  private static final Logger LOG = LoggerFactory.getLogger(CreateMode.class);

  private boolean ephemeral;
  private boolean sequential;
  private int flag;

  ...
}
```     

---  

<div id="zknamespace"></div>  

## Zookeeper 서버 구성도  

![zookeeper service](./pics/zkservice.jpg)  

- 각각의 서버는 다른 서버의 정보를 가지고 있음
- client는 하나의 서버랑 연결  
  - 커넥션을 유지 (sends requests, gets response, gets watch events, sends heart beats)  
  - 만약 서버와 연결이 끊어지면 다른 서버랑 통신
- Zookeeper 서버는 Leader와 Follower로 구성  
  - 자동으로 Leader 선정 & 모든 데이터 저장을 주도  
  - Client에서 Server(Follower)로 데이터 저장을 시도 할 때,  
    Server(Follower) -> Server(Leader) -> Server(Follower) 로 데이터 전달  
    => 팔로어 중 과반수의 팔로어로 부터 쓸 수 있다는 응답을 받으면 쓰도록 지시
  - 모든 서버에 동일한 데이터가 저장된 후 클라이언트에게 응답(동기 방식)
- 서버 간의 데이터 불일치가 발생하면 데이터 보정이 필요  
=> 과반수의 룰을 적용하기 때문에 홀수로 구성하는 것이 데이터 정합성 측면에서 유리  

---  

<div id="usage"></div>

## Basic usage  

> Download  

http://zookeeper.apache.org/releases.html

> Configuration  

```
$ tar -zxf zookeeper-3.4.6.tar.gz
$ cd zookeeper-3.4.6
$ vi conf/zoo.cfg
```  

> zoo.cfg  

```
tickTime = 2000
dataDir = /path/to/zookeeper/data
clientPort = 2181
initLimit = 5
syncLimit = 2
```  

> Start server  

```
$ bin/zkServer.sh start
```

> Start cli  

```
$ bin/zkCli.sh
```  

> Stop server  

```
$ bin/zkServer.sh stop
```  

---  

<div id="ref"></div>  

## Reference  

- zookeepr home  
  - https://zookeeper.apache.org  
  - http://zookeeper.apache.org/doc/current/zookeeperOver.html
  - https://zookeeper.apache.org/doc/current/zookeeperInternals.html

- 블로그  
  - https://engkimbs.tistory.com/660  
  - https://d2.naver.com/helloworld/294797
  - https://d2.naver.com/helloworld/583580
