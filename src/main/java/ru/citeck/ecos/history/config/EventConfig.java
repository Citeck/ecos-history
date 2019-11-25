package ru.citeck.ecos.history.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.citeck.ecos.events.EventConnection;
import ru.citeck.ecos.events.data.dto.record.RecordEventDto;
import ru.citeck.ecos.history.service.RecordsFacadeService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Roman Makarskiy
 */
@Slf4j
@Configuration
public class EventConfig {

    private ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ApplicationProperties appProps;
    private final RecordsFacadeService facadeService;

    public EventConfig(ApplicationProperties appProps, RecordsFacadeService facadeService) {
        this.appProps = appProps;
        this.facadeService = facadeService;
    }

    @Bean
    @Profile("!test")
    public EventConnection eventConnection() {
        return new EventConnection.Builder()
            .host(appProps.getEvent().getHost())
            .port(appProps.getEvent().getPort())
            .username(appProps.getEvent().getUsername())
            .password(appProps.getEvent().getPassword())
            .build();
    }

    //TODO: implement support multiple tenant id
    @Bean
    public CommandLineRunner registerEventHistoryProcessor(EventConnection eventConnection) {
        return args -> {
            try {
                eventConnection.receive("record.#", "attribute-facade", "local-ecos", (s, delivery, channel) -> {
                    try {
                        String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);

                        if (log.isDebugEnabled()) {
                            String info = "\n===========RECEIVE RECORD EVENT HISTORY============";
                            info += "\nconsumerTag: " + s;
                            info += "\nmsg: " + msg;
                            info += "\nchannel: " + channel;
                            info += "\n===========/RECEIVE RECORD EVENT HISTORY============";
                            log.debug(info);
                        }

                        RecordEventDto dto = OBJECT_MAPPER.readValue(msg, RecordEventDto.class);

                        facadeService.save(dto);
                    } catch (Throwable e) {
                        log.error("Failed process event", e);
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                    }

                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed register event history processor", e);
            }
        };
    }

}
