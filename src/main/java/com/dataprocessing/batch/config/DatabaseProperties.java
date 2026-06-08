package com.dataprocessing.batch.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.springframework.util.Assert.hasText;

@ConfigurationProperties(prefix = "batch.datasource")
public record DatabaseProperties(
    String url,
    String username,
    String password
) implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        hasText(url, "batch.datasource.url must be given");
        hasText(username, "batch.datasource.username must be given");
        hasText(password, "batch.datasource.password must be given");
    }
}
