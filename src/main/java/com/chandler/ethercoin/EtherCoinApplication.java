package com.chandler.ethercoin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EtherCoinApplication {

	public static void main(String[] args) {
		SpringApplication.run(EtherCoinApplication.class, args);
//		new SpringApplicationBuilder(CucoinApplication.class).web(true).run(args);
	}
}
