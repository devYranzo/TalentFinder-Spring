package dev.enekoyranzo.talentfinder.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Sustituye al almacén S3 real por la copia en memoria en todos los tests de integración. */
@TestConfiguration
public class TestStorageConfig {

    @Bean
    @Primary
    InMemoryCvStorage inMemoryCvStorage() {
        return new InMemoryCvStorage();
    }
}