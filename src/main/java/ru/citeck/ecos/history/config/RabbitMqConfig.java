package ru.citeck.ecos.history.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Roman Makarskiy
 */
@Configuration
public class RabbitMqConfig {

    public static final String SEND_NEW_RECORD_QUEUE = "send_new_record_queue";
    public static final String SEND_NEW_RECORDS_QUEUE = "send_new_records_queue";
    public static final String DELETE_RECORDS_BY_DOCUMENT = "delete_records_by_document_queue";

    @Bean
    public Queue sendNewRecordQueue() {
        return new Queue(SEND_NEW_RECORD_QUEUE);
    }

    @Bean
    public Queue sendNewRecordsQueue() {
        return new Queue(SEND_NEW_RECORDS_QUEUE);
    }

    @Bean
    public Queue deleteRecordsByDocumentQueue() {
        return new Queue(DELETE_RECORDS_BY_DOCUMENT);
    }

}
