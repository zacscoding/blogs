# 주키퍼(zookeeper) 분산 락처리

하나의 어플리케이션에서는 같은 JVM 위에서 동작하기 때문에  

`java.util.concurrent` 패키지의 Lock이나 Semaphore를 이용하여 하나의  

스레드만 동작하게 할 수 있습니다. 하지만 이중화, 오토 스케일링 등과 같이 다른 물리  

서버에서 동작 할 경우 위의 Lock, Semaphore 인스턴스를 이용하여 하나의 스레드만이  

task를 수행 하도록 할 수 없습니다.  

이를 해결하기 위해  

**1. `-Dtask1.enabled=true`과 같이 설정으로 하나의 WAS만 작업을 수행한다.**  

가장 간단한 방법이지만, 대부분 HA 구성을 위해 2대 이상의 서버가 동작하고 있고  

해당 서버의 장애가 발생할 경우 해당 작업은 수행되지 않습니다. 또한 k8s 등을 이용해서 Replica Set을 구성한 경우 파드간에 설정을 다르게 줘야하는 어려움이 있습니다.  

**2. Spring batch cluster 모드(quartz 등)로 작업을 수행한다.**  
해당 방법도 좋지만 이번 포스팅에서는 주키퍼 활용이 목적이므로 다루지 않습니다 :(

**3. 분산락을 사용한다.**  

Spring Integration에서 [locks 패키지](https://github.com/spring-projects/spring-integration/tree/main/spring-integration-core/src/main/java/org/springframework/integration/support/locks) `LockRegistry`를 제공하며 레디스, 주키퍼, JDBC 등의 구현체가 존재합니다.  

물론 운영 환경이면 해당 컴포넌트를 이용하겠지만, 주키퍼를 위한 포스팅이므로 직접 [Curator](https://github.com/apache/curator)를 이용해서 구현해보겠습니다.(간단합니다..!)  

---  

## Curator를 이용해서 분산락 구현하기

> CuratorFramework Bean 생성하기  

우선 아래와 같이 `CuratorFramework` 클래스를 빈으로 등록하겠습니다.  

```Java
@RequiredArgsConstructor
@Configuration
public class ZookeeperConfiguration {

    private final ZookeeperProperties properties;

    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        RetryPolicy retryPolicy = new RetryNTimes(
                properties.getMaxRetries(),
                properties.getSleepMsBetweenRetries());

        final CuratorFramework client =
                CuratorFrameworkFactory.newClient(properties.getConnectString(), retryPolicy);

        client.start();
        return client;
    }
    ...    
}
```  

<br>  

> LockRegistry Bean 등록  

다음으로 위에서 생성한 `CuratorFramework` Bean을 사용하는 `LockRegistry` 클래스를 생성하겠습니다.  

해당 클래스는 추후에 나오는 `Mutex`라는 클래스를 생성하는 역할을 합니다.(예제를 위한 코드이므로 인터페이스 대신 클래스로 생성합니다)  

추후의 `Mutex`라는 클래스는 아래의 `ROOT_LOCK_PATH(/lock/mutex/)` 하위의 `/lock/mutex/{taskId}`와 같은 락 경로를 사용합니다.  

```java
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

@Slf4j
@Component
@RequiredArgsConstructor
public class LockRegistry {

    private static final String ROOT_LOCK_PATH = "/lock/mutex/";

    // required
    private final CuratorFramework curatorFramework;

    /**
     * 주어진 taskId를 기반으로 {@link Mutex}를 생성합니다.
     *
     * @param taskId : Lock 획득에 대한 키
     * @param timeout : Lock 획득 시도 시간
     * @param timeUnit : Lock 획득 시도 시간 단위
     *
     * @return {@link Mutex}
     */
    public Mutex createMutex(String taskId, long timeout, TimeUnit timeUnit) {
        final String path = ROOT_LOCK_PATH + taskId;
        final InterProcessSemaphoreMutex mutex = new InterProcessSemaphoreMutex(curatorFramework, path);

        return new Mutex(mutex, path, timeout, timeUnit);
    }
}
```

> Mutex 클래스 작성

다음으로는 `Mutex` 라는 실제로 Lock 획득을 시도하는 클래스를 생성합니다.  

아래 주석에서 확인할 수 있듯이 mutex.acquireLock() 호출 후 `true(Lock 획득 성공)`를 반환하면 작업을 수행하고 `finally` 구문을 통해 락을 `release(반납)`합니다.  

```java
/**
 * Zookeeper {@link InterProcessSemaphoreMutex} 기반의 Mutex를 나타냅니다.
 *
 * <pre>{@code
 * Mutex mutex = registry.createMutex("job1", 3, TimeUnit.SECONDS);
 * try {
 *     if (mutex.acquireLock()) {
 *         // do work if success to acquire a lock.
 *         return;
 *     }
 *     // handle failure of acquiring a lock.
 * } finally {
 *     mutex.releaseLock();
 * }
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class Mutex {

    // required
    private final InterProcessSemaphoreMutex mutex;
    private final String path;
    private final long timeout;
    private final TimeUnit timeUnit;

    public boolean acquireLock() {
        // Check already locked.
        if (mutex.isAcquiredInThisProcess()) {
            logger.warn("Mutex-{} try to acquire a lock twice", path);
            return false;
        }

        try {
            return mutex.acquire(timeout, timeUnit);
        } catch (Exception e) {
            logger.error("Exception occur while acquiring {} lock.", path);
            return false;
        }
    }

    public boolean releaseLock() {
        // Check already has a lock.
        if (!mutex.isAcquiredInThisProcess()) {
            logger.warn("Mutex-{} not locked", path);
            return false;
        }

        try {
            mutex.release();
            return true;
        } catch (Exception e) {
            logger.warn("Exception occur while releasing a {} lock", path, e);
        }
        return false;
    }

    public boolean isAcquired() {
        return mutex.isAcquiredInThisProcess();
    }
}
```

---  

## 분산락 예제 작성하기

다음으로는 위의 예제를 활용한 Task를 작성하겠습니다.  

> 테스트를 위한 StubResource 작성  

아래의 `StubResource`는 락을 획득하면 `useResource()`라는 메소드를 호출합니다.  

`AtomicBoolean`를 이용하여 하나의 Task(스레드)만이 호출했는지 확인합니다. 만약 2개 이상의 스레드에서 호출했다면, `IllegalStateException` 예외를 전가합니다.  

그 다음 임의의 sleep() 함수를 호출하여 해당 메소드를 종료합니다.  

```java
public class StubResource {
    private final AtomicBoolean isUsed = new AtomicBoolean(false);
    private final long sleepMillisRange;

    public StubResource(long sleepMillisRange) {
        this.sleepMillisRange = sleepMillisRange;
    }

    public void useResource() {
        if (!isUsed.compareAndSet(false, true)) {
            throw new IllegalStateException("Resouce already used from another client");
        }

        try {
            TimeUnit.MILLISECONDS.sleep(new Random().nextInt((int) sleepMillisRange));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            isUsed.compareAndSet(true, false);
        }
    }
}
```

> Lock 기반의 Task 클래스 작성하기

아래와 같이 `Task::doWork(taskId, timeout, timeUnit)` 함수를 멀티 스레드에서 실행합니다.  

해당 메소드는 `taskId`로 락 획득을 시도합니다. 이때 (timeout, timeUnit)만큼 락 획득 시도를 기다립니다.  

만약 락 획득에 성공하면 위에서 작성한 `StubResource::useResource()`를 호출합니다(여기서 하나의 스레드만이 호출함을 체크합니다.)  

```java
@Slf4j
@RequiredArgsConstructor
public class Task {

    private final LockRegistry lockRegistry;
    private final StubResource resource;
    private final String workerId;
    private final CountDownLatch countDownLatch;
    private final TaskResult result = new TaskResult();

    /**
     * 주어진 taskId로 락 획득을 시도합니다. 만약 락 획득을 성공하면 {@link StubResource#useResource()} 메소드를 호출합니다.
     * 락 획득을 실패하면 작업을 종료합니다.
     */
    public void doWork(String taskId, long timeout, TimeUnit timeUnit) {
        result.workerId = workerId;
        result.taskId = taskId;

        // Mutex 생성
        final Mutex mutex = lockRegistry.createMutex(taskId, timeout, timeUnit);

        try {
            result.timeline.attemptAt = LocalDateTime.now();

            // 락 획득 시도 > 실패
            if (!mutex.acquireLock()) {
                result.timeline.failureAt = LocalDateTime.now();
                throw new Exception(String.format("Failed to acquire a lock. timeout: %d, unit: %s", timeout, timeUnit));
            }

            // 락 획득 성공
            result.timeline.acquireAt = LocalDateTime.now();
            result.acquired = true;
            resource.useResource();
        } catch (Exception e) {
            result.exception = e.getMessage();
        } finally {
            if (mutex.isAcquired()) {
                result.timeline.leaseAt = LocalDateTime.now();
                mutex.releaseLock();
            }
            countDownLatch.countDown();
        }
    }

    public TaskResult getTaskResult() {
        return result;
    }

    @Data
    public static class TaskResult {
        String workerId;
        String taskId;
        boolean acquired;
        String exception;
        TaskResultTimeline timeline = new TaskResultTimeline();
    }

    @Data
    public static class TaskResultTimeline {
        LocalDateTime attemptAt;
        LocalDateTime acquireAt;
        LocalDateTime failureAt;
        LocalDateTime leaseAt;
    }
}
```  

> 멀티 스레드 기반 Lock 테스트 하기

아래와 같이 하나의 `StubResource`를 사용하고 10개의 Worker 스레드를 실행합니다. `CountDownLatch`를 이용하여 모든 스레드 종료를 기다립니다.  

```java
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({ "test", "locking" })
public class LockTest {

    @Autowired
    LockRegistry lockRegistry;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    public void testLockExample() throws Exception {
        // Setup
        final StubResource resource = new StubResource(2000L);
        final int workers = 10;
        final CountDownLatch countDownLatch = new CountDownLatch(workers);
        final String taskId = "job1";
        final List<Task> tasks = new ArrayList<>();

        // Run tasks
        for (int i = 0; i < workers; i++) {
            final Task task = new Task(lockRegistry, resource, String.format("Worker-%d", i + 1), countDownLatch);
            tasks.add(task);

            Thread t = new Thread(() -> {
                task.doWork(taskId, 3, TimeUnit.SECONDS);
            });
            t.setDaemon(true);
            t.start();
        }

        // Wait for completion
        countDownLatch.await();

        // Print result
        List<TaskResult> results = tasks.stream().map(Task::getTaskResult).collect(Collectors.toList());
        TestHelper.out(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
    }
}
```

위의 예제를 실행하면 아래와 같은 결과를 콘솔을 통해 확인할 수 있습니다. 여기서 실패 Worker는 제외하고 성공한 Worker 기준으로 acquireAt 기준으로 정렬했습니다.  

```
[
  {
    "workerId": "Worker-10",
    "taskId": "job1",
    "acquired": true,
    "timeline": {
      "attemptAt": "2021-08-06T23:39:24.18",
      "acquireAt": "2021-08-06T23:39:24.517",
      "leaseAt": "2021-08-06T23:39:24.668"
    }
  },
  {
    "workerId": "Worker-9",
    "taskId": "job1",
    "acquired": true,
    "timeline": {
      "attemptAt": "2021-08-06T23:39:24.18",
      "acquireAt": "2021-08-06T23:39:24.688",
      "leaseAt": "2021-08-06T23:39:26.564"
    }
  },
  {
    "workerId": "Worker-2",
    "taskId": "job1",
    "acquired": true,
    "timeline": {
      "attemptAt": "2021-08-06T23:39:24.18",
      "acquireAt": "2021-08-06T23:39:26.579",
      "leaseAt": "2021-08-06T23:39:27.639"
    }
  }
]
```  

첫번째 Worker-10이 `23:39:24.517`에 락을 획득하여 `23:39:24.668`에 반납했습니다. 다음으로 Worker-9가 `23:39:24.688` 시간에 락을 획득하여  

`StubResource`를 사용하고 `23:39:26.564` 시간에 반납한 것을 확인할 수 있습니다.  

---  

## 마치며  

이번 포스팅에서는 Zookeeper와 Curator를 이용하여 분산락을 획득하는 예제를 살펴보았습니다.  

주키퍼 공식문서에는 락이나 리더 선출 등의 여러가지 [recipes](https://zookeeper.apache.org/doc/current/recipes.html#sc_recipes_Locks)이 기술되어 있습니다.  

해당 내용은 실제 [Zookeeper Project](https://github.com/apache/zookeeper/tree/master/zookeeper-recipes/zookeeper-recipes-lock)에 구현되어 있고 Curator를 통해 더 쉽게 사용할 수 있습니다.  

다음 포스팅에서는 위의 Recipes 중 Lock에 대해 설명하고 Golang을 통한 구현을 포스팅하겠습니다. 감사합니다 :)

## Reference  

- http://zookeeper.apache.org/
- https://curator.apache.org/