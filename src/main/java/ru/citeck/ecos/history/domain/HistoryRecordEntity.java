package ru.citeck.ecos.history.domain;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Data
@Entity(name = HistoryRecordEntity.ENTITY_NAME)
@Table(name = "HISTORY_RECORDS")
@SequenceGenerator(name = "entity_id_gen", sequenceName = "HISTORY_RECORDS_SEQ", allocationSize = 1)
public class HistoryRecordEntity implements Serializable {

    private static final long serialVersionUID = -3354822461532742388L;

    public static final String ENTITY_NAME = "HistoryRecord";

    @Id
    @GeneratedValue(generator = "entity_id_gen", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Basic(optional = false)
    @Column(name = "history_event_id")
    private String historyEventId;
    public static final String HISTORY_EVENT_ID = "historyEventId";

    @Basic(optional = false)
    @Column(name = "document_id")
    private String documentId;
    public static final String DOCUMENT_ID = "documentId";

    @Basic(optional = false)
    @Column(name = "event_type")
    private String eventType;
    public static final String EVENT_TYPE = "eventType";

    @Column(length = 6000)
    private String comments;
    public static final String COMMENTS = "comments";

    @Basic
    private String version;
    public static final String VERSION = "version";

    @Basic(optional = false)
    @Column(name = "creation_time")
    private Date creationTime;
    public static final String CREATION_TIME = "creationTime";

    @Basic(optional = false)
    private String username;
    public static final String USERNAME = "username";

    @Basic(optional = false)
    @Column(name = "user_id")
    private String userId;
    public static final String USER_ID = "userId";

    @Column(name = "task_title", length = 1000)
    private String taskTitle;
    public static final String TASK_TITLE = "taskTitle";

    @Column(name = "task_role")
    private String taskRole;
    public static final String TASK_ROLE = "taskRole";

    @Column(name = "task_outcome", length = 6000)
    private String taskOutcome;
    public static final String TASK_OUTCOME = "taskOutcome";

    @Column(name = "task_outcome_name")
    private String taskOutcomeName;
    public static final String TASK_OUTCOME_NAME = "taskOutcomeName";

    @Column(name = "task_definition_key")
    private String taskDefinitionKey;
    public static final String TASK_DEFINITION_KEY = "taskDefinitionKey";

    @Column(name = "task_type")
    private String taskType;
    public static final String TASK_TYPE = "taskType";

    @Column(name = "full_task_type")
    private String fullTaskType;
    public static final String FULL_TASK_TYPE = "fullTaskType";

    @Column(name = "initiator")
    private String initiator;
    public static final String INITIATOR = "initiator";

    @Column(name = "workflow_instance_id")
    private String workflowInstanceId;
    public static final String WORKFLOW_INSTANCE_ID = "workflowInstanceId";

    @Column(name = "workflow_description", length = 6000)
    private String workflowDescription;
    public static final String WORKFLOW_DESCRIPTION = "workflowDescription";

    @Column(name = "task_event_instance_id")
    private String taskEventInstanceId;
    public static final String TASK_EVENT_INSTANCE_ID = "taskEventInstanceId";

    @Column(name = "document_version")
    private String documentVersion;
    public static final String DOCUMENT_VERSION = "documentVersion";

    @Column(name = "property_name")
    private String propertyName;
    public static final String PROPERTY_NAME = "propertyName";

    @Column(name = "expected_perform_time")
    private Integer expectedPerformTime;
    public static final String EXPECTED_PERFORM_TIME = "expectedPerformTime";

    @Column(name = "task_form_key")
    private String taskFormKey;
    public static final String TASK_FORM_KEY = "taskFormKey";

    @Column(name = "doc_type")
    private String docType;
    public static final String DOC_TYPE = "docType";

    @Column(name = "doc_status_name")
    private String docStatusName;
    public static final String DOC_STATUS_NAME = "docStatusName";

    @Column(name = "doc_status_title")
    private String docStatusTitle;
    public static final String DOC_STATUS_TITLE = "docStatusTitle";

    @Column(name = "last_task_comment", length = 6000)
    private String lastTaskComment;
    public static final String LAT_TASK_COMMENT = "lastTaskComment";

    private static List<String> attributeNames;

    public static boolean isAttributeNameValid(String attributeName) {
        if (attributeNames == null) {
            attributeNames = Arrays.asList(HISTORY_EVENT_ID, DOCUMENT_ID, EVENT_TYPE, COMMENTS, VERSION,
                CREATION_TIME, USERNAME, USER_ID, TASK_TITLE, TASK_ROLE, TASK_TITLE, TASK_OUTCOME,
                TASK_DEFINITION_KEY, TASK_FORM_KEY, TASK_EVENT_INSTANCE_ID, TASK_OUTCOME_NAME, FULL_TASK_TYPE,
                INITIATOR, WORKFLOW_INSTANCE_ID, WORKFLOW_DESCRIPTION, DOC_STATUS_NAME, DOC_STATUS_TITLE,
                DOC_TYPE, DOCUMENT_VERSION, LAT_TASK_COMMENT, PROPERTY_NAME, EXPECTED_PERFORM_TIME,
                ENTITY_NAME);
        }
        if (attributeNames.contains(attributeName)){
            return true;
        }
        return false;
    }
}
