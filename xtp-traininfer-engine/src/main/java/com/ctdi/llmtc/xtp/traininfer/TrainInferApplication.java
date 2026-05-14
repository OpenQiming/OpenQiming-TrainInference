package com.ctdi.llmtc.xtp.traininfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrainInferApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainInferApplication.class, args);
    }

}
