package imbuy.category;

import org.springframework.boot.SpringApplication;

public class TestCategoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(CategoryServiceApplication::main)
                .with(TestContainersConfig.class)
                .run(args);
    }
}