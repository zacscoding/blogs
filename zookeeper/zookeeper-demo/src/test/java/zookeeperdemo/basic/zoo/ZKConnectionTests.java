package zookeeperdemo.basic.zoo;

import java.util.concurrent.TimeUnit;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

import zookeeperdemo.common.TestHelper;

public class ZKConnectionTests {

    @Test
    public void testZKConnection() throws Exception {
        String host = "localhost:2183";
        ZKConnection conn = new ZKConnection(host);
        ZooKeeper zk = conn.connect();
        zk.close();
    }

    @Test
    public void testCurator() throws Exception {
        String host = "localhost:2181";
        RetryPolicy retryPolicy = new RetryNTimes(3, 2000);
        CuratorFramework client = CuratorFrameworkFactory.newClient(host, retryPolicy);
        client.start();

        InterProcessMutex lock = new InterProcessMutex(client, "/mylock");
        if (lock.acquire(1000, TimeUnit.MILLISECONDS)) {
            TestHelper.out("## Success to acquire a lock");
            try {
                InterProcessMutex lock2 = new InterProcessMutex(client, "/mylock");
                if (lock2.acquire(1000, TimeUnit.MILLISECONDS)) {
                    try {
                        TestHelper.out("## Success to acquire a lock in internal");
                    } finally {
                        lock2.release();
                    }
                } else {
                    TestHelper.out("## Failed to acquire a lock in internal");
                }
            } finally {
                lock.release();
            }
        } else {
            TestHelper.out("## Failed to acquire a lock");
        }
        client.close();
    }
}
