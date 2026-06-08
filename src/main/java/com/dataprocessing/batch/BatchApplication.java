package com.dataprocessing.batch;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Date;
import java.util.TimeZone;

@Slf4j
@SpringBootApplication
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }

    @PostConstruct
    void steUtcTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("BatchApplication running in UTC timezone at : {}", new Date());
    }

    @Configuration
    @Profile("test")
    @ComponentScan(lazyInit = true)
    static class ConfigForShorterBootTimeForTests {
    }
}
