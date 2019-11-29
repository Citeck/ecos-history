package ru.citeck.ecos.history.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
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

    private final Recover recover = new Recover();

    private RecordsProperties records;
    private String tryHeaderForUsername;

    @Getter
    @Setter
    public static class Alfresco {
        //TODO: add support multiple tenant id
        private String TENANT_ID = HistoryDefault.Alfresco.TENANT_ID;
    }

    @Data
    public static class Recover {
        private Boolean scheduled;
        private String sourceFolder;
        private String errorsFolder;
    }

}
