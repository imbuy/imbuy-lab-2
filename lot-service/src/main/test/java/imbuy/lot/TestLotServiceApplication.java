package imbuy.lot;

import org.springframework.boot.SpringApplication;

public class TestLotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(LotServiceApplication::main)
                .with(TestContainersConfig.class)
                .run(args);
    }
}