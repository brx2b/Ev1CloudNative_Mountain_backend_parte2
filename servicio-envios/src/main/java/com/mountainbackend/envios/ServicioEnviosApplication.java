package com.mountainbackend.envios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ServicioEnviosApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicioEnviosApplication.class, args);
	}

}
