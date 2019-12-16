package zookeeperdemo.example.locking;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/locking")
public class LockingExampleController {

    // required
    private final CuratorFramework curatorFramework;
    private final Map<String, InterProcessSemaphoreMutex> mutexMap = new HashMap<>();

    @GetMapping("/{taskId}/{timeout}")
    public ResponseEntity doWork(@PathVariable("taskId") String taskId, @PathVariable("timeout") long timeout) {
        logger.info("Try to acquire {} lock with {}[secs] timeout", taskId, timeout);
        boolean acquired = acquireLock(taskId, timeout, TimeUnit.SECONDS);
        logger.info(">> result : {}", acquired);

        if (acquired) {
            final int sleepSeconds = new Random().nextInt(10) + 60;
            Thread task = new Thread(() -> {
                logger.info("### Start to do work... during {} secs", sleepSeconds);
                try {
                    for (int i = 0; i < sleepSeconds; i++) {
                        logger.info("### Do working... {}", i);
                        TimeUnit.SECONDS.sleep(1L);
                    }
                } catch (Exception e) {
                    // ignore
                }
                releaseLock(taskId);
            });
            task.setDaemon(true);
            task.start();
            return ResponseEntity.ok("Success to acquire " + taskId + " lock. and will do task during "
                                     + sleepSeconds + " seconds");
        }

        return ResponseEntity.ok("Failed to acquire " + taskId + " lock.");
    }

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
}
