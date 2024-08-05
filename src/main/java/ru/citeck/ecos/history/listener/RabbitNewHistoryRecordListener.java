package ru.citeck.ecos.history.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.repository.HistoryRecordRepository;
import ru.citeck.ecos.history.service.HistoryRecordService;
import ru.citeck.ecos.rabbitmq.RabbitMqConn;
import ru.citeck.ecos.rabbitmq.RabbitMqConnProvider;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service("rabbitNewHistoryRecordListener")
@ConditionalOnClass({EnableRabbit.class})
public class RabbitNewHistoryRecordListener {

    public static final String SEND_NEW_RECORD_QUEUE = "send_new_record_queue";
    public static final String SEND_NEW_RECORDS_QUEUE = "send_new_records_queue";
    public static final String DELETE_RECORDS_BY_DOCUMENT = "delete_records_by_document_queue";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HistoryRecordService historyRecordService;
    private final HistoryRecordRepository historyRecordRepository;
    private final RabbitMqConnProvider rabbitMqConnProv;

    @PostConstruct
    public void init() {
        RabbitMqConn connection = rabbitMqConnProv.getConnection();
        if (connection == null) {
            throw new RuntimeException("RabbitMQ connection is not found");
        }

        connection.doWithNewChannel(channel -> {
            channel.declareQueue(SEND_NEW_RECORD_QUEUE, true);

            channel.addAckedConsumer(SEND_NEW_RECORD_QUEUE, Delivery.class, (msg, headers) -> {
                sendNewRecordListener(new String(msg.getContent().getBody(), StandardCharsets.UTF_8));
            });
        });

        connection.doWithNewChannel(channel -> {
            channel.declareQueue(SEND_NEW_RECORDS_QUEUE, true);

            channel.addAckedConsumer(SEND_NEW_RECORDS_QUEUE, Delivery.class, (msg, headers) -> {
                sendNewRecordsListener(new String(msg.getContent().getBody(), StandardCharsets.UTF_8));
            });
        });
        connection.doWithNewChannel(channel -> {
            channel.declareQueue(DELETE_RECORDS_BY_DOCUMENT, true);

            channel.addAckedConsumer(DELETE_RECORDS_BY_DOCUMENT, Delivery.class, (msg, headers) -> {
                deleteRecordsByDocumentListener(new String(msg.getContent().getBody(), StandardCharsets.UTF_8));
            });
        });
    }

    /**
     * @param message Message (json-object)
     */
    public synchronized void sendNewRecordListener(String message) throws IOException, ParseException {
        Map<String, String> resultMap = OBJECT_MAPPER.readValue(message, new TypeReference<HashMap<String, String>>() {
        });
        historyRecordService.saveOrUpdateRecord(new HistoryRecordEntity(), resultMap);
    }

    /**
     * @param message Message (json-object)
     */
    public synchronized void sendNewRecordsListener(String message) throws IOException, ParseException {
        historyRecordService.saveOrUpdateRecords(message);
    }

    /**
     * @param message Document uuid
     */
    public synchronized void deleteRecordsByDocumentListener(String message) {
        List<HistoryRecordEntity> records = historyRecordRepository.getRecordsByDocumentId(message);
        historyRecordRepository.deleteAll(records);
    }
}
