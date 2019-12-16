package zookeeperdemo.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Component
@ConfigurationProperties(prefix = "zookeeper")
public class ZookeeperProperties {

    private String connectString;
    private int maxRetries;
    private int sleepMsBetweenRetries;
}