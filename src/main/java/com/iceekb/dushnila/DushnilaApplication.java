package com.iceekb.dushnila;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DushnilaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DushnilaApplication.class, args);
    }
}
