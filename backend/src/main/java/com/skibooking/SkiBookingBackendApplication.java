package com.skibooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkiBookingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkiBookingBackendApplication.class, args);
	}

}
