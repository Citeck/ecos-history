package ru.citeck.ecos.history.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class HistoryRecordDto implements Serializable {

    private static final long serialVersionUID = -8215328451338606217L;

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
    private String taskOutcomeName;
    private String taskDefinitionKey;
    private String taskType;
    private String documentId;

    public HistoryRecordDto() {
    }

    public HistoryRecordDto(HistoryRecordDto other) {
        this.uuid = other.uuid;
        this.historyEventId = other.historyEventId;
        this.comments = other.comments;
        this.version = other.version;
        this.username = other.username;
        this.userId = other.userId;
        this.eventType = other.eventType;
        this.creationTime = other.creationTime;
        this.taskTitle = other.taskTitle;
        this.taskRole = other.taskRole;
        this.taskOutcome = other.taskOutcome;
        this.taskOutcomeName = other.taskOutcomeName;
        this.taskDefinitionKey = other.taskDefinitionKey;
        this.taskType = other.taskType;
        this.documentId = other.documentId;
    }
}
