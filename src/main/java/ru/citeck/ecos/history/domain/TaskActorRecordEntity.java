package ru.citeck.ecos.history.domain;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;

@Data
@ToString(exclude = {"task", "actor"})
@Entity(name = TaskActorRecordEntity.ENTITY_NAME)
@Table(name = "tasks_actors")
public class TaskActorRecordEntity implements Serializable {

    private static final long serialVersionUID = 1783197389310573369L;

    public static final String ENTITY_NAME = "TasksActorsRecord";

    @EmbeddedId
    private TaskActorRecordEntityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_records_id")
    @MapsId("taskRecordsId")
    private TaskRecordEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_records_id")
    @MapsId("actorRecordsId")
    private ActorRecordEntity actor;

}
