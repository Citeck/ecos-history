package ru.citeck.ecos.history.service.task.impl;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import static ru.citeck.ecos.history.service.HistoryRecordService.*;

public class HandlersTestUtils {

    private static final SecureRandom random = new SecureRandom();

    public static Map<String, String> generateTaskBaseParams(String id) {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(DOCUMENT_ID, id);
        requestParams.put(CREATION_TIME, "03.04.2010 23:00:00");
        requestParams.put(USERNAME, "admin");
        requestParams.put(USER_ID, "user-id");

        requestParams.put(TASK_EVENT_INSTANCE_ID, "activiti&" + random.nextInt());

        return requestParams;
    }

}
