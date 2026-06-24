package com.cqu.springboot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cqu.springboot.mapper")

public class LibrarySysApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibrarySysApplication.class, args);
    }

}
