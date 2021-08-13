package zookeeperdemo.example.locking;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
