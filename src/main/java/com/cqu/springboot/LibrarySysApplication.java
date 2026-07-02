package com.cqu.springboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.cqu.springboot.mapper")
@EnableScheduling
@EnableAsync
public class LibrarySysApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibrarySysApplication.class, args);
    }

}
