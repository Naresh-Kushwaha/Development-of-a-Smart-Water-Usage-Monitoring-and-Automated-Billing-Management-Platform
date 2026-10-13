package com.smartwater;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartWaterApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartWaterApplication.class, args);
    }
}
