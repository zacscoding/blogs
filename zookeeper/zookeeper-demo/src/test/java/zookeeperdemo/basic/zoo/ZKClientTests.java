package zookeeperdemo.basic.zoo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import zookeeperdemo.common.TestHelper;

/**
 *
 * @GitHub : https://github.com/zacscoding
 */
public class ZKClientTests {

    // String host = "192.168.79.130:2181";
    String host = "127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183";
    ZKConnection zkConnection1;
    ZooKeeper zooKeeper1;

    ZKConnection zkConnection2;
    ZooKeeper zooKeeper2;

    @BeforeClass
    public static void classSetUp() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.WARN);
    }

    @Before
    public void setUp() throws Exception {
        zkConnection1 = new ZKConnection(host);
        zooKeeper1 = zkConnection1.connect();

        zkConnection2 = new ZKConnection(host);
        zooKeeper2 = zkConnection2.connect();
    }

    @After
    public void tearDown() throws Exception {
        if (zkConnection1 != null) {
            zkConnection1.close();
        }

        if (zkConnection2 != null) {
            zkConnection2.close();
        }
    }

    /**
     * /MyFirstZnode
     *
     * @throws Exception
     */
    @Test
    public void testCreateGetSetDataWithPersistent() throws Exception {
        String path = "/MyFirstZnode";
        byte[] data = "initialized data".getBytes(StandardCharsets.UTF_8);
        byte[] updatedData = "updated data".getBytes(StandardCharsets.UTF_8);

        // 1) exists => null
        Stat stat = zooKeeper1.exists(path, true);
        if (stat != null) {
            zooKeeper1.delete(path, stat.getVersion());
        }

        // 2) create Persistent node
        TestHelper.out("Try to create path : %s, data : %s", path, new String(data, StandardCharsets.UTF_8));
        String result = zooKeeper1.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        TestHelper.out("> %s", result);

        assertThat(stat = zooKeeper1.exists(path, false)).isNotNull();

        List<ACL> acl1 = zooKeeper1.getACL(path, null);

        TestHelper.out("Try to get acl.");
        TestHelper.out("> acl from zookeeper1");
        for (ACL acl : acl1) {
            TestHelper.out(">> perms : %d, schema : %s, id : %s",
                           acl.getPerms(), acl.getId().getScheme(), acl.getId().getId());
        }

        // 3) get data
        TestHelper.out("Try to get data path : %s", path);
        byte[] readData = zooKeeper2.getData(path, event -> {
            switch (event.getType()) {
                case None:
                    TestHelper.out("None event occur..");
                    break;
                case NodeCreated:
                    TestHelper.out("NodeCreated event occur..");
                    break;
                case NodeDeleted:
                    TestHelper.out("NodeDeleted event occur..");
                    break;
                case NodeDataChanged:
                    TestHelper.out("NodeDataChanged event occur..");
                    break;
                case NodeChildrenChanged:
                    TestHelper.out("NodeChildrenChanged event occur..");
                    break;
                case DataWatchRemoved:
                    TestHelper.out("DataWatchRemoved event occur..");
                    break;
                case ChildWatchRemoved:
                    TestHelper.out("ChildWatchRemoved event occur..");
                    break;
            }
        }, stat);
        TestHelper.out("> %s", new String(readData, StandardCharsets.UTF_8));

        // 4) set data
        TestHelper.out("Try to set data path : %s", path);
        stat = zooKeeper1.setData(path, updatedData, stat.getVersion());
        TestHelper.out("> success to set %s", stat);

        // 5) delete znode
        TestHelper.out("Try to delete data path : %s", path);
        zooKeeper1.delete(path, stat.getVersion());
        TestHelper.out("> success to delete");

        assertThat(zooKeeper2.exists(path, false)).isNull();
    }

    @Test
    public void testCreateAndDeleteEphemeralNode() throws Exception {
        String path = "/MySecondZnode";
        byte[] data = "initialized data".getBytes(StandardCharsets.UTF_8);

        // 2) create Persistent node
        TestHelper.out("Try to create path : %s, data : %s", path, new String(data, StandardCharsets.UTF_8));
        String result = zooKeeper1.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        TestHelper.out("> %s", result);

        // 3) get data
        TestHelper.out("Try to get data path : %s", path);
        CountDownLatch nodeDeletedLatch = new CountDownLatch(1);
        byte[] readData = zooKeeper2.getData(path, event -> {
            switch (event.getType()) {
                case None:
                    TestHelper.out("None event occur..");
                    break;
                case NodeCreated:
                    TestHelper.out("NodeCreated event occur..");
                    break;
                case NodeDeleted:
                    TestHelper.out("NodeDeleted event occur..");
                    nodeDeletedLatch.countDown();
                    break;
                case NodeDataChanged:
                    TestHelper.out("NodeDataChanged event occur..");
                    break;
                case NodeChildrenChanged:
                    TestHelper.out("NodeChildrenChanged event occur..");
                    break;
                case DataWatchRemoved:
                    TestHelper.out("DataWatchRemoved event occur..");
                    break;
                case ChildWatchRemoved:
                    TestHelper.out("ChildWatchRemoved event occur..");
                    break;
            }
        }, null);
        TestHelper.out("> %s", new String(readData, StandardCharsets.UTF_8));

        // 4) disconnect zookeeper1
        TestHelper.out("Try to disconnect zkConnection1");
        zkConnection1.close();

        nodeDeletedLatch.await();
    }
}
