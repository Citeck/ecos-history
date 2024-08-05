package ru.citeck.ecos.history.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Properties specific to History.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 */
@Data
@ConfigurationProperties(prefix = "ecos-history", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Recover recover = new Recover();
    private final Alfresco alfresco = new Alfresco();

    // legacy properties. Should not be used
    @Deprecated
    private Map<String, Object> records;
    @Deprecated
    private String tryHeaderForUsername;
    /* ====================================*/

    private boolean deferredActorsJobEnabled;

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
