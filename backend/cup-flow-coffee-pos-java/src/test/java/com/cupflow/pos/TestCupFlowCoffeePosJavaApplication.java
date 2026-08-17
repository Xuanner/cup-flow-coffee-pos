package com.cupflow.pos;

import org.springframework.boot.SpringApplication;

public class TestCupFlowCoffeePosJavaApplication {

    public static void main(String[] args) {
        SpringApplication.from(CupFlowCoffeePosJavaApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
