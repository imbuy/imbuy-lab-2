package imbuy.bid;

import org.springframework.boot.SpringApplication;

public class TestBidServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(BidServiceApplication::main)
                .with(TestContainersConfig.class)
                .run(args);
    }
}