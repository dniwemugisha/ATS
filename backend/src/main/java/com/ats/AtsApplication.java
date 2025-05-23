package com.ats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = "com.ats")
@EnableAspectJAutoProxy
@EnableScheduling
public class AtsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AtsApplication.class, args);
    }
} 