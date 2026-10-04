package com.vote;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.vote"})
@MapperScan(basePackages = {"com.vote.mappers"})
public class voteApplication {
    public static void main(String[] args) {
        SpringApplication.run(voteApplication.class, args);
    }
}
