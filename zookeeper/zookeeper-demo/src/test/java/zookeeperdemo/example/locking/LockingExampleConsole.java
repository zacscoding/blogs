package zookeeperdemo.example.locking;

import java.util.Date;
import java.util.List;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import zookeeperdemo.basic.zoo.ZKConnection;
import zookeeperdemo.common.LogLevelUtil;

/**
 *
 */
public class LockingExampleConsole {

    String host = "192.168.79.130:2181";
    ZKConnection zkConnection;
    ZooKeeper zooKeeper;

    @Before
    public void setUp() throws Exception {
        LogLevelUtil.setWarn();
        zkConnection = new ZKConnection(host);
        zooKeeper = zkConnection.connect();
    }

    @After
    public void tearDown() throws Exception {
        zooKeeper.close();
    }

    @Test
    public void readData() throws Exception {
        String path = "/lock/mutex/aaa";
        readData(path);
    }

    private void readData(String path) throws Exception {
        System.out.println(">> Check " + path);

        Stat stat = zooKeeper.exists(path, false);
        if (stat == null) {
            System.out.println("empty stats");
            return;
        }

        byte[] data = zooKeeper.getData(path, false, stat);
        if (data.length > 0) {
            System.out.println("==> Read " + path + "(" + data.length + ") : " + new String(data));
        }

        List<String> children = zooKeeper.getChildren(path, false);
        for (String child : children) {
            readData(path + "/" + child);
        }
    }
}
