package com.foody.delivery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeliveryApplication {

	public static void main(String[] args) throws IOException {
		// O driver do SQLite cria o arquivo, mas não a pasta onde ele fica.
		Files.createDirectories(Path.of("data"));
		SpringApplication.run(DeliveryApplication.class, args);
	}

}
