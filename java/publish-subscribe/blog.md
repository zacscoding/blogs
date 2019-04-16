## Publish / Subscribe 패턴 알아보기  

모든 소스 코드는 Github (<a href=""><a>)에 있습니다.  

스타는 사랑입니다 :)  

publish / subscribe 관련 패턴은 

- <a href="#blocking-queue">Blocking Queue</a>

---  

<div id="blocking-queue"></div>  

## BlockingQueue를 이용하여 Publish / Subscribe 패턴 구현하기

자바의 java.util.concurrent.BlockingQueue 인터페이스 구현체를 통해서 Publish / Subscribe를 구현할 수 있습니다.  

우선 Publisher와 Subscriber가 공통의 BlockingQueue 구현체 인스턴스를 가지고 메시지 Publish(publisher::offer())  
  
메시지 Subcribe(subscriber::take())를 통해서 메시지를 전달할 수 있습니다.  

아래는 간단한 Publisher, Subscriber, Runner 예제 입니다.

> BlockingQueueSubscriber.java  

```aidl
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

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
                // final String message = queue.take();
                final String message = queue.poll();
                logger.info("[Consumer] {}", message);
                consumerOptional.ifPresent(consumer -> consumer.accept(message));
            }
        } catch (Exception e) {
            logger.error("Exception occur while taking messages", e);
            return;
        }

    }
}
``` 

> BlockingQueuePublisher.java  

```aidl
package blog.publishsubscribe.blockingqueue;

import com.sun.media.jfxmedia.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

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
```  

> BlockingQueueRunner  

```aidl
package blog.publishsubscribe.blockingqueue;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

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
        // Output
        // 00:13:46.534 [Thread-1] INFO  b.p.b.BlockingQueuePublisher - [Producer] 00:13:46-message-1
        // 00:13:46.539 [Thread-0] INFO  b.p.b.BlockingQueueSubscriber - [Consumer] 00:13:46-message-1
        // 00:13:47.540 [Thread-1] INFO  b.p.b.BlockingQueuePublisher - [Producer] 00:13:47-message-2
        // 00:13:47.540 [Thread-0] INFO  b.p.b.BlockingQueueSubscriber - [Consumer] 00:13:47-message-2
        // 00:13:48.541 [Thread-1] INFO  b.p.b.BlockingQueuePublisher - [Producer] 00:13:48-message-3
        // 00:13:48.541 [Thread-0] INFO  b.p.b.BlockingQueueSubscriber - [Consumer] 00:13:48-message-3
    }
}
```  

=> BlockingQueue 인터페이스는 상황에 따라 아래와 같은 큐를 쓸 수 있습니다.
- java.util.concurrent.ArrayBlockingQueue (Array List 기반 FIFO)
- java.util.concurrent.LinkedBlockingQueue (Linked List 기반 FIFO)  
- java.util.concurrent.PriorityBlockingQueue (우선순위큐)  

=> 위의 Subscriber에서 queue.take() 메소드로 가져와야지만 메시지가 없을 때 해당 스레드가 블로킹 되어 있고  
poll() 메소드를 호출 할 경우에는 poll() 메소드 호출 시점에서 큐가 비었으면 null을 반환합니다.  

=> LinkedBlockingQueue를 간략하게 분석해보면,  
아래와 같이 count.get() == 0 이면 (큐가 비어있으면) notEmpty.await() 함수를 호출해서  
blocking 상태에 있게 되고, offer 메소드에서 보듯이 맨 마지막에 signalNotEmpty(); 메소드  
호출을 통해서 blocking 되어 있는 스레드를 깨워줍니다.  

> java.util.concurrent.LinkedBlockingQueue.java

```aidl
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }
    
    public boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException {

        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            while (count.get() == capacity) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(new Node<E>(e));
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
        return true;
    }    
```  

---  

