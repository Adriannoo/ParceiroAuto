package br.edu.uniamerica.parceiro_auto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Habilita a descoberta das interfaces de integracao com APIs externas.
@EnableFeignClients
@SpringBootApplication
public class Projeto4periodoApplication {

	public static void main(String[] args) {
		SpringApplication.run(Projeto4periodoApplication.class, args);
	}

}
