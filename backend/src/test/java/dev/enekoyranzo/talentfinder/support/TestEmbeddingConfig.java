package dev.enekoyranzo.talentfinder.support;

import dev.enekoyranzo.talentfinder.indexing.TextEmbedder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Sustituye al modelo real por el embebedor falso en todos los tests de integración. */
@TestConfiguration
public class TestEmbeddingConfig {

    @Bean
    TextEmbedder textEmbedder() {
        return new FakeTextEmbedder();
    }
}