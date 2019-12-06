package ru.citeck.ecos.history.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"startEvent", "assignEvent", "completeEvent", "actors"})
@Entity(name = TaskRecordEntity.ENTITY_NAME)
@Table(name = "task_records")
@SequenceGenerator(name = "entity_id_gen", sequenceName = "task_records_seq", allocationSize = 1)
public class TaskRecordEntity extends AbstractAuditingEntity {

    private static final long serialVersionUID = -8191988742802285812L;

    public static final String ENTITY_NAME = "TaskRecord";
    public static final String FIELD_TASK_ID = "taskId";

    @Id
    @GeneratedValue(generator = "entity_id_gen", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Basic(optional = false)
    @Column(name = "task_id")
    private String taskId;

    @Basic
    @Column(name = "workflow_id")
    private String workflowId;

    @Basic
    @Column(name = "assignee")
    private String assignee;

    @Basic
    @Column(name = "assignee_manager")
    private String assigneeManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_event_id")
    private HistoryRecordEntity startEvent;

    @Basic
    @Column(name = "start_event_date")
    private Date startEventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assign_event_id")
    private HistoryRecordEntity assignEvent;

    @Basic
    @Column(name = "assign_event_date")
    private Date assignEventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complete_event_id")
    private HistoryRecordEntity completeEvent;

    @Basic
    @Column(name = "complete_event_date")
    private Date completeEventDate;

    @Basic
    @Column(name = "due_date")
    private Date dueDate;

    @Basic
    @Column(name = "document_id")
    private String documentId;

    @Basic
    @Column(name = "document_type")
    private String documentType;

    @Basic
    @Column(name = "document_status_name")
    private String documentStatusName;

    @Basic
    @Column(name = "document_status_title")
    private String documentStatusTitle;

    @Basic
    @Column(name = "completion_comment")
    private String completionComment;

    @Basic
    @Column(name = "form_key")
    private String formKey;

    @Basic
    @Column(name = "last_task_comment", length = 1000)
    private String lastTaskComment;

    @OneToMany(mappedBy = "task")
    private List<TaskActorRecordEntity> actors;

}
