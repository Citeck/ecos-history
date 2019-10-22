package ru.citeck.ecos.history.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to History.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@Data
@ConfigurationProperties(prefix = "ecos-history", ignoreUnknownFields = false)
public class ApplicationProperties {

    private Recover recover = new Recover();

    @Data
    public static class Recover {
        private Boolean scheduled;
        private String sourceFolder;
        private String errorsFolder;
    }

}
