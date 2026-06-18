package com.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 智能学习助手应用入口
 */
@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@EnableAsync
@EnableScheduling
public class AiStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiStudyApplication.class, args);
    }
}
