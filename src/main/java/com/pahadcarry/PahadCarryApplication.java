package com.pahadcarry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PahadCarryApplication {

    public static void main(String[] args) {
        SpringApplication.run(PahadCarryApplication.class, args);
    }
}
