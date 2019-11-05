package ru.citeck.ecos.history.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class TaskActorRecordEntityId implements Serializable {

    private static final long serialVersionUID = -8142679631871270444L;

    @Basic(optional = false)
    @Column(name = "task_records_id")
    private Long taskRecordsId;

    @Basic(optional = false)
    @Column(name = "actor_records_id")
    private Long actorRecordsId;

}
