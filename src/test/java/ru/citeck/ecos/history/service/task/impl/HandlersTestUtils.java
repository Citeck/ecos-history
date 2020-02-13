package ru.citeck.ecos.history.service.task.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static ru.citeck.ecos.history.service.HistoryRecordService.*;

public class HandlersTestUtils {

    public static Map<String, String> generateTaskBaseParams(String id) {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(DOCUMENT_ID, id);
        requestParams.put(CREATION_TIME, "03.04.2010 23:00:00");
        requestParams.put(USERNAME, "admin");
        requestParams.put(USER_ID, "user-id");

        Random random = new Random();

        requestParams.put(TASK_EVENT_INSTANCE_ID, "activiti&" + random.nextInt());

        return requestParams;
    }

}
