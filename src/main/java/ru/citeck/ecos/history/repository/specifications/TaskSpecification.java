package ru.citeck.ecos.history.repository.specifications;

import org.springframework.data.jpa.domain.Specification;
import ru.citeck.ecos.history.domain.ActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskActorRecordEntity;
import ru.citeck.ecos.history.domain.TaskRecordEntity;

import javax.persistence.criteria.*;
import java.util.Collection;
import java.util.List;

public class TaskSpecification {

    public static Specification<TaskRecordEntity> isActive(boolean active) {
        return (entityRoot, query, builder) -> {
            if (active) {
                return builder.isNull(entityRoot.get("completeEventDate"));
            } else {
                return builder.isNotNull(entityRoot.get("completeEventDate"));
            }
        };
    }

    public static Specification<TaskRecordEntity> hasDocumentType(String documentType) {
        return (entityRoot, query, builder) -> builder.equal(entityRoot.get("documentType"), documentType);
    }

    public static Specification<TaskRecordEntity> hasActor(List<String> actorList) {
        return (root, query, cb) -> {
            Join<TaskRecordEntity, TaskActorRecordEntity> taskActors = root.join("actors", JoinType.INNER);
            Join<TaskActorRecordEntity, ActorRecordEntity> actor = taskActors.join("actor", JoinType.INNER);
            Expression<Object> actorName = actor.get("actorName");
            return actorName.in(actorList);

//            return cb.in(in);


//            Join<TaskRecordEntity, ActorRecordEntity> actorsJoin = root.join("actors");
//            Expression<Object> actorName1 = actorsJoin.get("actorName");
//            Predicate in = actorName1.in(actorList);
//            return cb.in(in);

//            query.distinct(true);
//            Subquery<ActorRecordEntity> actorSubQuery = query.subquery(ActorRecordEntity.class);
//            Root<ActorRecordEntity> actor = actorSubQuery.from(ActorRecordEntity.class);
//            Expression<Collection<TaskRecordEntity>> actorTasks = actor.get("tasks");
//            actorSubQuery.select(actor);
//            Expression<Object> actorName = actor.get("actorName");
//            Predicate actorIn = actorName.in(actorList);
//            actorSubQuery.where(cb.in(actorIn), cb.isMember(root, actorTasks));
//            return cb.exists(actorSubQuery);

//            query.distinct(true);
//            Root<ActorRecordEntity> actor = query.from(ActorRecordEntity.class);
//            Expression<Collection<TaskRecordEntity>> actorTasks = actor.get("tasks");
//            Expression<Object> actorName = actor.get("actorName");
//            Predicate actorIn = actorName.in(actorList);
//            return cb.and(cb.in(actorIn), cb.isMember(root, actorTasks));
        };
    }

    public static Specification<TaskRecordEntity> hasDocumentStatus(String documentStatus) {
        return (entityRoot, query, builder) -> builder.equal(entityRoot.get("documentStatusName"), documentStatus);
    }

    public static Specification<TaskRecordEntity> hasDocument(String documentId) {
        return (entityRoot, query, builder) -> builder.equal(entityRoot.get("documentId"), documentId);
    }

}
