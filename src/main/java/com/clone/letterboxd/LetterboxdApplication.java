package com.clone.letterboxd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class LetterboxdApplication {

	public static void main(String[] args) {
		SpringApplication.run(LetterboxdApplication.class, args);
	}

}
