package com.animeverse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.animeverse.mapper")
public class AnimeVerseApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnimeVerseApplication.class, args);
    }
}
