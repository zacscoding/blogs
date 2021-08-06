package zookeeperdemo.example.locking;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
