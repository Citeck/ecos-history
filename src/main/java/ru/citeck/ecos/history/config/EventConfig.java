package ru.citeck.ecos.history.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.citeck.ecos.events.EventConnection;
import ru.citeck.ecos.events.data.dto.pasrse.EventDtoFactory;
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
    @Profile("!test")
    public CommandLineRunner registerEventHistoryProcessor(EventConnection eventConnection) {
        return args -> {
            try {
                eventConnection.receive("record.#", "attribute-facade", "local-ecos",
                    (consumerTag, message, channel) -> {
                        try {
                            String msg = new String(message.getBody(), StandardCharsets.UTF_8);
                            String routingKey = message.getEnvelope().getRoutingKey();

                            if (log.isDebugEnabled()) {
                                String info = "\n===========RECEIVE RECORD EVENT HISTORY============";
                                info += "\nroutingKey: " + routingKey;
                                info += "\nmsg: " + msg;
                                info += "\nchannel: " + channel;
                                info += "\n===========/RECEIVE RECORD EVENT HISTORY============";
                                log.debug(info);
                            }

                            RecordEventDto dto = EventDtoFactory.fromEventDtoMsg(msg);

                            facadeService.save(dto);
                        } catch (Throwable e) {
                            log.error("Failed process event", e);
                            channel.basicNack(message.getEnvelope().getDeliveryTag(), false, false);
                        }

                        channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                    });
            } catch (IOException e) {
                throw new RuntimeException("Failed register event history processor", e);
            }
        };
    }

}
