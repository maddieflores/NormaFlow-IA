package com.workflow.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Configuración del cliente AWS S3 (Ciclo 2 — CU-24 al 27).
 *
 * Las credenciales se leen EXCLUSIVAMENTE de variables de entorno
 * (aws.s3.access-key / aws.s3.secret-key en application.properties
 *  que a su vez leen AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY del entorno).
 *
 * Principio SRP: responsabilidad única de proveer el cliente S3 configurado.
 * Principio DIP: el resto del sistema depende del Bean S3Client/S3Presigner,
 *                no de la inicialización concreta de AWS SDK.
 */
@Slf4j
@Configuration
public class AwsS3Config {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    /**
     * Cliente S3 sincrónico para operaciones de subida, eliminación y listado.
     */
    @Bean
    public S3Client s3Client() {
        log.info("✅ Inicializando AWS S3Client — bucket región: {}", region);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    /**
     * Presigner para generar URLs pre-firmadas de descarga temporal.
     * Las URLs pre-firmadas permiten que el frontend descargue directamente
     * desde S3 sin pasar por el backend (mejor rendimiento, menos carga).
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
