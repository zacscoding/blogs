package zookeeperdemo.example.locking;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
