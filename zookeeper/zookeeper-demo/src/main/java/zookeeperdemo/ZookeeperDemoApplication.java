package zookeeperdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        excludeFilters = {
                @Filter(type = FilterType.REGEX, pattern = "zookeeperdemo.example\\..*")
        }
)
public class ZookeeperDemoApplication {

    public static void main(String[] args) {
        // enable locking
        System.setProperty("spring.profiles.active", "locking");
        SpringApplication.run(ZookeeperDemoApplication.class, args);
    }
}
