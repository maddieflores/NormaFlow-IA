package com.workflow.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableScheduling
public class BackendApplication {
	public static void main(String[] args) {
		// Carga el .env si existe (desarrollo local).
		// En producción (Cloud Run) las variables ya vienen como env vars del sistema.
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()   // no falla si no hay .env (producción)
				.systemProperties()  // las expone como System.getProperty()
				.load();

		// También las pone como variables de entorno del proceso para que
		// Spring las resuelva con ${VAR} en application.properties
		dotenv.entries().forEach(e ->
				System.setProperty(e.getKey(), e.getValue())
		);

		SpringApplication.run(BackendApplication.class, args);
	}
}
