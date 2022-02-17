package ru.citeck.ecos.history.api.records;

import ru.citeck.ecos.events2.type.RecordCreatedEvent;
import ru.citeck.ecos.history.dto.HistoryRecordDto;
import ru.citeck.ecos.records2.RecordRef;


public class HistoryRecordTestData {

    public static final String HISTORY_APP_ID =  "history";
    public static String HISTORY_RECORD_ID = "test-uuid";
    public static String PROP_DOC_ID = "test-doc";
    public static String PROP_ID = "id";
    public static String PROP_COLUMNS = "columns";

    public static final HistoryRecordDto testHistoryRecordDto = new HistoryRecordDto();
    public static final RecordRef testTypeRef = RecordRef.create("emodel", "type", "testType");

    static {
        testHistoryRecordDto.setUuid(HISTORY_RECORD_ID);
        testHistoryRecordDto.setCreationTime(System.currentTimeMillis());
        testHistoryRecordDto.setEventType(RecordCreatedEvent.TYPE);
        testHistoryRecordDto.setHistoryEventId("testEventId");
        testHistoryRecordDto.setComments("Test history record.");
        testHistoryRecordDto.setUsername("testUsername");
        testHistoryRecordDto.setUserId("testUserId");
        testHistoryRecordDto.setDocumentId(PROP_DOC_ID);
    }

    public static HistoryRecordDto getTestHistoryRecord() {
        return new HistoryRecordDto(testHistoryRecordDto);
    }

    public static String getEmptyId() {
        return HISTORY_APP_ID + "/" + HistoryRecordRecordsDao.ID + "@";
    }

    public static HistoryRecordDto getNewHistoryRecord() {
        HistoryRecordDto historyRecordDto = new HistoryRecordDto(testHistoryRecordDto);
        historyRecordDto.setUuid(null);
        historyRecordDto.setCreationTime(System.currentTimeMillis());
        return historyRecordDto;
    }
}
