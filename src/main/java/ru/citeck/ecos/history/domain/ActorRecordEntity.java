package ru.citeck.ecos.history.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"tasks"})
@Entity(name = ActorRecordEntity.ENTITY_NAME)
@Table(name = "actor_records")
@SequenceGenerator(name = "entity_id_gen", sequenceName = "actor_records_seq", allocationSize = 1)
public class ActorRecordEntity extends AbstractAuditingEntity {

    private static final long serialVersionUID = -7457280939312773970L;

    public static final String ENTITY_NAME = "ActorRecord";

    @Id
    @GeneratedValue(generator = "entity_id_gen", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Basic(optional = false)
    @Column(name = "actor_name")
    private String actorName;

    @OneToMany(mappedBy = "actor")
    private List<TaskActorRecordEntity> tasks;

}

