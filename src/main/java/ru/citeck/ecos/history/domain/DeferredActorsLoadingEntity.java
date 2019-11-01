package ru.citeck.ecos.history.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity(name = DeferredActorsLoadingEntity.ENTITY_NAME)
@Table(name = "deferred_actors_loading")
@SequenceGenerator(name = "entity_id_gen", sequenceName = "deferred_actors_loading_seq", allocationSize = 1)
public class DeferredActorsLoadingEntity extends AbstractAuditingEntity {

    private static final long serialVersionUID = 7223285426049675285L;

    public static final String ENTITY_NAME = "DeferredActorsLoading";

    @Id
    @GeneratedValue(generator = "entity_id_gen", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Basic(optional = false)
    @Column(name = "task_id")
    private String taskId;

}
