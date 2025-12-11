package com.platform.talent.jobposting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class JobPostingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobPostingServiceApplication.class, args);
    }
}

