# 주키퍼(zookeeper) 분산 락처리

하나의 어플리케이션에서는 같은 JVM 위에서 동작하기 때문에  

`java.util.concurrent` 패키지의 Lock이나 Semaphore 등으로 하나의  

스레드만 동작하게 할 수 있습니다. 하지만 이중화와 같이 다른 물리  

서버에서 동작 할 경우 위의 인스턴스를 이용하여 하나의 스레드만이  

task를 수행 하도록 할 수 없습니다.  

간단하게 `-Dtask1.enabled=true` 프로퍼티로 스케줄러를 Enable, Disable 할 수 있지만  

해당 서버의 장애로 오류가 날 경우 해당 작업을 수행 할 수 없습니다.  

이런 상황에서 주키퍼를 이용하여 아래와 같이 분산 락처리가 가능합니다.  


> CuratorFramework 생성  


```Java

...

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

> Lock acquire & lease



```Java

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/locking")
public class LockingExampleController {

    // required
  private final CuratorFramework curatorFramework;
  private final Map<String, InterProcessSemaphoreMutex> mutexMap = new HashMap<>();

  ...

  private boolean acquireLock(String taskId, long timeout, TimeUnit timeUnit) {
      final String path = "/lock/mutex/" + taskId;
      final InterProcessSemaphoreMutex mutex = new InterProcessSemaphoreMutex(curatorFramework, path);

      try {
          if (mutex.acquire(timeout, timeUnit)) {
              mutexMap.put(taskId, mutex);
              return true;
          }
      } catch (Exception e) {
          logger.warn("Exception occur while acquiring {} lock.", taskId, e);
      }

      return false;
  }

  private void releaseLock(String taskId) {
    InterProcessSemaphoreMutex mutex = mutexMap.remove(taskId);

    if (mutex == null || !mutex.isAcquiredInThisProcess()) {
        logger.warn("{} didn't acquired lock.", taskId);
        return;
    }

    try {
        mutex.release();
    } catch (Exception e) {
        logger.warn("Exception occur while releasing a {} lock", taskId, e);
    }
  }
```  


위와 같이 `InterProcessSemaphoreMutex` 를 유지하는 이유는 락을 획득 한 이후  

내부에서 `org.apache.curator.framework.recipes.locks.Lease` 인스턴스를  

유지하기 때문입니다.  

실제로 "/lock/mutex/aaa" path로 lock을 요청 한 뒤 zookeeper를 조회하면  

아래와 같은 Path에 임시 노드(Ephemeral)로 생성을 요청합니다.

- /lock/mutex/aaa
- /lock/mutex/aaa/locks
- /lock/mutex/aaa/leases
- /lock/mutex/aaa/leases/_c_9575b095-e158-4b38-af76-0e689daa9a58-lease-0000000000

## Reference  

- https://curator.apache.org/
