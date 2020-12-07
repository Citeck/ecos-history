package ru.citeck.ecos.history.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.citeck.ecos.records3.RecordsProperties;

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
    private final Event event = new Event();
    private final Alfresco alfresco = new Alfresco();

    private RecordsProperties records;
    private String tryHeaderForUsername;
    private boolean deferredActorsJobEnabled;

    @Getter
    @Setter
    public static class Event {

        private String host = HistoryDefault.Event.HOST;
        private int port = HistoryDefault.Event.PORT;
        private String username = HistoryDefault.Event.USERNAME;
        private String password = HistoryDefault.Event.PASSWORD;

    }

    @Getter
    @Setter
    public static class Alfresco {
        //TODO: add support multiple tenant id
        private String tenantId = HistoryDefault.Alfresco.TENANT_ID;
    }

    @Data
    public static class Recover {

        private Boolean scheduled;
        private String sourceFolder;
        private String errorsFolder;
    }

}
