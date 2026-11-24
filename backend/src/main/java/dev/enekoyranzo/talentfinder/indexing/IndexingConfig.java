package dev.enekoyranzo.talentfinder.indexing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class IndexingConfig {
}