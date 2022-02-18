package ru.citeck.ecos.history.api.records;

import ru.citeck.ecos.history.dto.HistoryRecordDto;

public class HistoryRecordTestData {

    public static final String HISTORY_APP_ID =  "history";
    public static String HISTORY_RECORD_ID = "test-uuid";
    public static String ADMIN = "admin";
    public static String PROP_DOC_ID = "test-doc";
    public static String PROP_ID = "id";
    public static String RECORD_CREATED_EVENT_TYPE = "record-created";
    public static String RECORD_CHANGED_EVENT_TYPE = "record-changed";
    public static String PREDICATE_VAL = "val";
    public static String PREDICATE_TYPE = "t";
    public static String PREDICATE_TYPE_AND = "and";
    public static String PREDICATE_TYPE_EQUAL = "eq";
    public static String PREDICATE_TYPE_STARTS = "starts";
    public static String PREDICATE_TYPE_GREATER = "gt";

    public static final HistoryRecordDto testHistoryRecordDto = new HistoryRecordDto();

    static {
        testHistoryRecordDto.setUuid(HISTORY_RECORD_ID);
        testHistoryRecordDto.setCreationTime(System.currentTimeMillis());
        testHistoryRecordDto.setEventType(RECORD_CREATED_EVENT_TYPE);
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
