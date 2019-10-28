package ru.citeck.ecos.history.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.citeck.ecos.history.domain.TaskRecordEntity;

import java.util.List;

public interface TaskRecordRepository extends
    CrudRepository<TaskRecordEntity, Long>,
    JpaSpecificationExecutor<TaskRecordEntity> {

    @Query("SELECT task FROM " + TaskRecordEntity.ENTITY_NAME + " as task " +
        "WHERE task." + TaskRecordEntity.FIELD_TASK_ID + " = :" + TaskRecordEntity.FIELD_TASK_ID)
    TaskRecordEntity getByTaskId(@Param(TaskRecordEntity.FIELD_TASK_ID) String taskId);

    List<TaskRecordEntity> getByDocumentId(String documentId);

}
