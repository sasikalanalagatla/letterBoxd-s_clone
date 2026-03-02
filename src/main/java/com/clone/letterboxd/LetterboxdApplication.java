package com.clone.letterboxd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LetterboxdApplication {

	public static void main(String[] args) {
		SpringApplication.run(LetterboxdApplication.class, args);
	}

}
