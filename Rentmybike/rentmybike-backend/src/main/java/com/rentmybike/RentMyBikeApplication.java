package com.rentmybike;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import com.rentmybike.common.config.AppProperties;

/**
 * Entry point for the RentMyBike Spring Boot application.
 * Einstiegspunkt für die RentMyBike Spring Boot-Anwendung.
 *
 * <p>Annotations explained / Annotationen erklärt:
 * <ul>
 *   <li>{@code @SpringBootApplication} — enables component scan, auto-configuration, config</li>
 *   <li>{@code @EnableJpaAuditing} — activates @CreatedDate and @LastModifiedDate in BaseEntity</li>
 *   <li>{@code @EnableAsync} — enables @Async on EmailService methods (non-blocking emails)</li>
 *   <li>{@code @EnableConfigurationProperties} — binds AppProperties @ConfigurationProperties class</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing       // Activates createdAt/updatedAt auto-fill / Aktiviert createdAt/updatedAt Auto-Befüllung
@EnableAsync             // Non-blocking email sending / Nicht-blockierender E-Mail-Versand
@EnableConfigurationProperties(AppProperties.class)
public class RentMyBikeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentMyBikeApplication.class, args);
    }
}
