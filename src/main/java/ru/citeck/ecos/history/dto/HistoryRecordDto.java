package ru.citeck.ecos.history.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class HistoryRecordDto implements Serializable {

    private static final long serialVersionUID = -2176188346685254300L;

    private String uuid;
    private String historyEventId;
    private String comments;
    private String version;
    private String username;
    private String userId;
    private String eventType;
    private Long creationTime;
    private String taskTitle;
    private String taskRole;
    private String taskOutcome;
    private String taskDefinitionKey;
    private String taskType;
    private String documentId;
}
