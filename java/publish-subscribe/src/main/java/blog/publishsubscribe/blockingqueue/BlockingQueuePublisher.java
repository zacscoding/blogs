package blog.publishsubscribe.blockingqueue;

import com.sun.media.jfxmedia.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * BlockingQueue 기반 Publisher
 *
 * @GitHub : https://github.com/zacscoding
 */
@Slf4j
public class BlockingQueuePublisher extends Thread {

    private BlockingQueue<String> queue;
    private DateTimeFormatter formatter;
    private int messageCount;

    public BlockingQueuePublisher(BlockingQueue<String> queue) {
        Objects.requireNonNull(queue, "queue must be not null");
        this.queue = queue;
        this.formatter = DateTimeFormatter.ofPattern("[HH:mm:ss]");
        super.setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String message = new StringBuilder()
                    .append(LocalDateTime.now().format(formatter))
                    .append("-message-")
                    .append(++messageCount)
                    .toString();

                logger.info("[Producer] {}", message);
                queue.offer(message);
                TimeUnit.SECONDS.sleep(1L);
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}
