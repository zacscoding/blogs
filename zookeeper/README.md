# Zookeeper  

- <a href="#zookeeper">Zookeeper 란?</a>
- <a href="#zknamespace">Zookeeper 데이터 모델</a>
- <a href="#ensemble">Zookeeper 서버 구성도</a>
- <a href="#usage-cli">설치 및 CLI 기본 사용</a>
- <a href="#compose">도커 컴포즈를 이용한 주키퍼 실행</a>
- <a href="#usage-java-client">Java client 기본 사용</a>
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

<div id="usage-cli"></div>

## CLI 기본 사용법

> Download  

http://zookeeper.apache.org/releases.html  에서 다운로드

> Configuration  

다운로드 받은 후 압축을 해제하면 `zoo.cfg` 파일이 확인

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

<div id="compose"></div>  

## 도커 컴포즈를 이용한 주키퍼 실행  

아래와 같이 3개의 주키퍼 실행

> docker-compose.yamkl

```
version: '3.1'

services:
  zoo1:
    image: zookeeper
    restart: always
    hostname: zoo1
    ports:
      - 2181:2181
    environment:
      ZOO_MY_ID: 1
      ZOO_SERVERS: server.1=0.0.0.0:2888:3888;2181 server.2=zoo2:2888:3888;2181 server.3=zoo3:2888:3888;2181
    volumes:
      - ./temp-zookeeper1/data:/data
      - ./temp-zookeeper1/datalog:/datalog

  zoo2:
    image: zookeeper
    restart: always
    hostname: zoo2
    ports:
      - 2182:2181
    environment:
      ZOO_MY_ID: 2
      ZOO_SERVERS: server.1=zoo1:2888:3888;2181 server.2=0.0.0.0:2888:3888;2181 server.3=zoo3:2888:3888;2181
    volumes:
      - ./temp-zookeeper2/data:/data
      - ./temp-zookeeper2/datalog:/datalog

  zoo3:
    image: zookeeper
    restart: always
    hostname: zoo3
    ports:
      - 2183:2181
    environment:
      ZOO_MY_ID: 3
      ZOO_SERVERS: server.1=zoo1:2888:3888;2181 server.2=zoo2:2888:3888;2181 server.3=0.0.0.0:2888:3888;2181
    volumes:
      - ./temp-zookeeper3/data:/data
      - ./temp-zookeeper3/datalog:/datalog
```  

> docker compose up

```
$ docker-compose up -d
```  

---  

<div id="usage-java-client"></div>  

## Java client 기본 사용  

> 의존성 추가  

```
// gradle
compile 'org.apache.zookeeper:zookeeper:3.5.5'

// maven
<!-- https://mvnrepository.com/artifact/org.apache.zookeeper/zookeeper -->
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.5.5</version>
</dependency>
```

> Zookeeper 클라이언트  

```
import static java.util.Objects.requireNonNull;

import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @GitHub : https://github.com/zacscoding
 */
@Slf4j
public class ZKConnection {

    private String host;
    private ZooKeeper zoo;
    private boolean connected;

    public ZKConnection(String host) {
        this.host = requireNonNull(host, "host");
    }

    public ZooKeeper connect() throws Exception {
        if (isConnected()) {
            throw new IllegalStateException("Already connected");
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        zoo = new ZooKeeper(host, 2000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getState() == KeeperState.SyncConnected) {
                    logger.info("Success to connect {}", host);
                    updateConnectionState(true);
                    countDownLatch.countDown();
                } else {
                    logger.debug("Receive new watched event : {}", event.getState());
                }
            }
        });

        countDownLatch.await();
        return zoo;
    }

    public boolean isConnected() {
        synchronized (this) {
            return connected;
        }
    }

    public void close() throws Exception {
        synchronized (this) {
            if (zoo != null) {
                zoo.close();
                zoo = null;
                connected = false;
            }
        }
    }

    private void updateConnectionState(boolean state) {
        synchronized (this) {
            connected = state;
        }
    }
}
```  

> 기본 사용법 테스트  

```  

/**
* "/MyFirstZnode" 라는 path에 persistent 노드를 생성하여
* 데이터 Setting/Getting 관련 사용법 테스트
* 1) "/MyFirstZnode" 라는 path가 존재하는지 체크    
* 2) Persistent 노드 생성
* 3) 데이터 조회하기 (노드 변경에 따른 이벤트 리스너를 등록할 수 있음)
* 4) "/MyFirstZnode"에 데이터 Setting
* 5) znode 삭제
*/
@Test
public void testCreateGetSetDataWithPersistent() throws Exception {
    String path = "/MyFirstZnode";
    byte[] data = "initialized data".getBytes(StandardCharsets.UTF_8);
    byte[] updatedData = "updated data".getBytes(StandardCharsets.UTF_8);

    // 1) "/MyFirstZnode" 라는 path가 존재하는지 체크    
    Stat stat = zooKeeper1.exists(path, true);
    if (stat != null) {
        zooKeeper1.delete(path, stat.getVersion());
    }

    // 2) Persistent 노드 생성
    TestHelper.out("Try to create path : %s, data : %s", path, new String(data, StandardCharsets.UTF_8));
    String result = zooKeeper1.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    TestHelper.out("> %s", result);

    assertThat(stat = zooKeeper1.exists(path, false)).isNotNull();

    List<ACL> acl1 = zooKeeper1.getACL(path, null);

    TestHelper.out("Try to get acl.");
    TestHelper.out("> acl from zookeeper1");
    for (ACL acl : acl1) {
        TestHelper.out(">> perms : %d, schema : %s, id : %s",
                       acl.getPerms(), acl.getId().getScheme(), acl.getId().getId());
    }

    // 3) 데이터 조회하기 (노드 변경에 따른 이벤트 리스너를 등록할 수 있음)
    TestHelper.out("Try to get data path : %s", path);
    byte[] readData = zooKeeper2.getData(path, event -> {
        switch (event.getType()) {
            case None:
                TestHelper.out("None event occur..");
                break;
            case NodeCreated:
                TestHelper.out("NodeCreated event occur..");
                break;
            case NodeDeleted:
                TestHelper.out("NodeDeleted event occur..");
                break;
            case NodeDataChanged:
                TestHelper.out("NodeDataChanged event occur..");
                break;
            case NodeChildrenChanged:
                TestHelper.out("NodeChildrenChanged event occur..");
                break;
            case DataWatchRemoved:
                TestHelper.out("DataWatchRemoved event occur..");
                break;
            case ChildWatchRemoved:
                TestHelper.out("ChildWatchRemoved event occur..");
                break;
        }
    }, stat);
    TestHelper.out("> %s", new String(readData, StandardCharsets.UTF_8));

    // 4) "/MyFirstZnode"에 데이터 Setting
    TestHelper.out("Try to set data path : %s", path);
    stat = zooKeeper1.setData(path, updatedData, stat.getVersion());
    TestHelper.out("> success to set %s", stat);

    // 5) znode 삭제
    TestHelper.out("Try to delete data path : %s", path);
    zooKeeper1.delete(path, stat.getVersion());
    TestHelper.out("> success to delete");

    assertThat(zooKeeper2.exists(path, false)).isNull();
}

/**
* "/MyFirstZnode" 라는 path에 ephemeral 노드를 생성하여
* 데이터 Setting/Getting 관련 사용법 테스트
* 1) ephemeral node 생성
* 2) 데이터 조회
* 3) disconnect zookeeper1 -> 세션 종료
* 4) ephemeral 노드 삭제 이벤트 대기
*/
@Test
public void testCreateAndDeleteEphemeralNode() throws Exception {
    String path = "/MySecondZnode";
    byte[] data = "initialized data".getBytes(StandardCharsets.UTF_8);

    // 1) ephemeral node 생성
    TestHelper.out("Try to create path : %s, data : %s", path, new String(data, StandardCharsets.UTF_8));
    String result = zooKeeper1.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
    TestHelper.out("> %s", result);

    // 2) 데이터 조회
    TestHelper.out("Try to get data path : %s", path);
    CountDownLatch nodeDeletedLatch = new CountDownLatch(1);
    byte[] readData = zooKeeper2.getData(path, event -> {
        switch (event.getType()) {
            case None:
                TestHelper.out("None event occur..");
                break;
            case NodeCreated:
                TestHelper.out("NodeCreated event occur..");
                break;
            case NodeDeleted:
                TestHelper.out("NodeDeleted event occur..");
                nodeDeletedLatch.countDown();
                break;
            case NodeDataChanged:
                TestHelper.out("NodeDataChanged event occur..");
                break;
            case NodeChildrenChanged:
                TestHelper.out("NodeChildrenChanged event occur..");
                break;
            case DataWatchRemoved:
                TestHelper.out("DataWatchRemoved event occur..");
                break;
            case ChildWatchRemoved:
                TestHelper.out("ChildWatchRemoved event occur..");
                break;
        }
    }, null);
    TestHelper.out("> %s", new String(readData, StandardCharsets.UTF_8));

    // 3) disconnect zookeeper1
    TestHelper.out("Try to disconnect zkConnection1");
    zkConnection1.close();

    4) ephemeral 노드 삭제 이벤트 대기
    nodeDeletedLatch.await();
}
```  

---  

다음번에는 curator 라이브러리를 이용하여 위에서 언급했던 주키퍼 활용 케이스별로  
소스 코드와 함께 업로드를 해보겠습니다 :)

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
