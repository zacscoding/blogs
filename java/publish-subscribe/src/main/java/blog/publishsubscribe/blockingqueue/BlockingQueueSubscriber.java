package blog.publishsubscribe.blockingqueue;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * BlockingQueue 기반 Consumer
 *
 * @GitHub : https://github.com/zacscoding
 */
@Slf4j
public class BlockingQueueSubscriber extends Thread {

    private BlockingQueue<String> queue;
    private Optional<Consumer<String>> consumerOptional;

    public BlockingQueueSubscriber(BlockingQueue<String> queue, Optional<Consumer<String>> consumerOptional) {
        Objects.requireNonNull(queue, "queue must be not null");
        this.queue = queue;
        this.consumerOptional = consumerOptional;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                final String message = queue.take();
                logger.info("[Consumer] {}", message);
                consumerOptional.ifPresent(consumer -> consumer.accept(message));
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                return;
            }
            logger.error("Exception occur while taking messages", e);
            return;
        }

    }
}
