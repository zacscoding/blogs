package blog.publishsubscribe.blockingqueue;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @GitHub : https://github.com/zacscoding
 */
public class BlockingQueueRunner {

    public static void main(String[] args) throws InterruptedException {
        final CountDownLatch readCount = new CountDownLatch(3);
        BlockingQueue queue = new LinkedBlockingQueue();
        BlockingQueueSubscriber subscriber = new BlockingQueueSubscriber(queue,
            Optional.of(message -> readCount.countDown()));
        BlockingQueuePublisher publisher = new BlockingQueuePublisher(queue);

        subscriber.start();
        publisher.start();

        readCount.await();

        publisher.interrupt();
        subscriber.interrupt();
    }
}
