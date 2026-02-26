package com.clone.letterboxd.config;

import org.springframework.boot.restclient.RestTemplateBuilder;   // ← this is the correct import in Boot 4.0+
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))     // ← no "set" prefix
                .readTimeout(Duration.ofSeconds(30))        // ← same here
                .build();
    }
}