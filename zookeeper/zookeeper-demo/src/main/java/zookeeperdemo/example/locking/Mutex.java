package zookeeperdemo.example.locking;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
