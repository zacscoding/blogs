package zookeeperdemo.configuration;

import javax.annotation.PostConstruct;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import zookeeperdemo.configuration.properties.ZookeeperProperties;

/**
 *
 */
@RequiredArgsConstructor
@Configuration
public class ZookeeperConfiguration {

    private final ZookeeperProperties properties;

    @Bean(destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        RetryPolicy retryPolicy = new RetryNTimes(
                properties.getMaxRetries(),
                properties.getSleepMsBetweenRetries());

        final CuratorFramework client =
                CuratorFrameworkFactory.newClient(properties.getConnectString(), retryPolicy);

        client.start();
        return client;
    }

    @Slf4j
    @Profile("locking")
    @Configuration
    @ComponentScan(basePackages = "zookeeperdemo.example.locking")
    public static class LockingConfiguration {

        @PostConstruct
        private void setUp() {
            logger.info("## Scan locking packages");
        }
    }
}
