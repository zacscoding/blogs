package zookeeperdemo.example.locking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import zookeeperdemo.common.TestHelper;
import zookeeperdemo.example.locking.Task.TaskResult;

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
