package ru.citeck.ecos.history.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;
import ru.citeck.ecos.history.repository.HistoryRecordRepository;
import ru.citeck.ecos.history.service.HistoryRecordService;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("rabbitNewHistoryRecordListener")
@ConditionalOnClass({EnableRabbit.class})
public class RabbitNewHistoryRecordListener {

    private static final String SEND_NEW_RECORD_QUEUE = "send_new_record_queue";
    private static final String SEND_NEW_RECORDS_QUEUE = "send_new_records_queue";
    private static final String DELETE_RECORDS_BY_DOCUMENT = "delete_records_by_document_queue";

    private HistoryRecordService historyRecordService;
    private HistoryRecordRepository historyRecordRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param message Message (json-object)
     */
    @RabbitListener(queues = SEND_NEW_RECORD_QUEUE)
    public void sendNewRecordListener(String message) throws IOException, ParseException {
        Map<String, String> resultMap = objectMapper.readValue(message, new TypeReference<HashMap<String, String>>() {
        });
        historyRecordService.saveOrUpdateRecord(new HistoryRecordEntity(), resultMap);
    }

    /**
     * @param message Message (json-object)
     */
    @RabbitListener(queues = SEND_NEW_RECORDS_QUEUE)
    public void sendNewRecordsListener(String message) throws IOException, ParseException {
        historyRecordService.saveOrUpdateRecords(message);
    }

    /**
     * @param message Document uuid
     */
    @RabbitListener(queues = DELETE_RECORDS_BY_DOCUMENT)
    public void deleteRecordsByDocumentListener(String message) {
        List<HistoryRecordEntity> records = historyRecordRepository.getRecordsByDocumentId(message);
        historyRecordRepository.deleteAll(records);
    }

    @Autowired
    public void setHistoryRecordService(HistoryRecordService historyRecordService) {
        this.historyRecordService = historyRecordService;
    }

    @Autowired
    public void setHistoryRecordRepository(HistoryRecordRepository historyRecordRepository) {
        this.historyRecordRepository = historyRecordRepository;
    }
}
