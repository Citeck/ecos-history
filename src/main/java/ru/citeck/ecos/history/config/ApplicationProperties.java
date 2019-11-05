package ru.citeck.ecos.history.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.citeck.ecos.records2.RecordsProperties;

/**
 * Properties specific to History.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@Data
@ConfigurationProperties(prefix = "ecos-history", ignoreUnknownFields = false)
public class ApplicationProperties {

    private RecordsProperties records;
    private Recover recover = new Recover();
    private String tryHeaderForUsername;

    @Data
    public static class Recover {
        private Boolean scheduled;
        private String sourceFolder;
        private String errorsFolder;
    }

}
