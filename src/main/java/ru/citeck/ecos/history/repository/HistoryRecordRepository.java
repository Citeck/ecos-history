package ru.citeck.ecos.history.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.history.domain.HistoryRecordEntity;

import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

public interface HistoryRecordRepository extends CrudRepository<HistoryRecordEntity, Long>,
    JpaSpecificationExecutor<HistoryRecordEntity> {

    /**
     * Get all history records
     *
     * @return List of history records
     */
    @Query("SELECT record FROM " + HistoryRecordEntity.ENTITY_NAME + " as record " +
        "ORDER BY record." + HistoryRecordEntity.CREATION_TIME)
    List<HistoryRecordEntity> getAllRecords(Pageable pageable);

    /**
     * Get all records by document id
     *
     * @param documentId Document id
     * @return List of history records
     */
    @Query("SELECT record FROM " + HistoryRecordEntity.ENTITY_NAME + " as record " +
        "WHERE record." + HistoryRecordEntity.DOCUMENT_ID + " = :documentId " +
        "ORDER BY record." + HistoryRecordEntity.CREATION_TIME)
    List<HistoryRecordEntity> getRecordsByDocumentId(@Param("documentId") String documentId);

    /**
     * Get history record by history event id
     *
     * @param historyEventId History event id
     * @return History record or null
     */
    @Query("SELECT record FROM " + HistoryRecordEntity.ENTITY_NAME + " as record " +
        "WHERE record." + HistoryRecordEntity.HISTORY_EVENT_ID + " = :historyEventId")
    HistoryRecordEntity getHistoryRecordByHistoryEventId(@Param("historyEventId") String historyEventId);

    /**
     * Get history records by username without filtering
     *
     * @param username Username
     * @return List of history records
     */
    @Query("SELECT record FROM " + HistoryRecordEntity.ENTITY_NAME + " as record " +
        "WHERE record." + HistoryRecordEntity.USERNAME + " = :username " +
        "ORDER BY record." + HistoryRecordEntity.CREATION_TIME)
    List<HistoryRecordEntity> getAllHistoryRecordsByUsername(
        @Param("username") String username,
        Pageable pageable
    );

    /**
     * Get history records by username with start date
     *
     * @param username  Username
     * @param startDate Start date
     * @return List of history records
     */
    @Query("SELECT record FROM " + HistoryRecordEntity.ENTITY_NAME + " as record " +
        "WHERE record." + HistoryRecordEntity.USERNAME + " = :username " +
        "AND cast(" + HistoryRecordEntity.CREATION_TIME + " as date) >= cast(:startDate as date) " +
        "ORDER BY record." + HistoryRecordEntity.CREATION_TIME)
    List<HistoryRecordEntity> getAllHistoryRecordsByUsernameWithStartDate(
        @Param("username") String username,
        @Param("startDate") Date startDate,
        Pageable pageable
    );

    /**
     * Get history records by username with start and end date
     *
     * @param username  Username
     * @param startDate Start date
     * @param endDate   End date
     * @return List of history records
     */
    @Query("SELECT record FROM " + HistoryRecordEntity.ENTITY_NAME + " as record " +
        "WHERE record." + HistoryRecordEntity.USERNAME + " = :username " +
        "AND cast(" + HistoryRecordEntity.CREATION_TIME + " as date) " +
        "BETWEEN cast(:startDate as date) AND cast(:endDate as date) " +
        "ORDER BY record." + HistoryRecordEntity.CREATION_TIME)
    List<HistoryRecordEntity> getAllHistoryRecordsByUsernameWithStartEndDate(
        @Param("username") String username,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        Pageable pageable
    );

    @Transactional
    @Modifying
    @Query("DELETE FROM " + HistoryRecordEntity.ENTITY_NAME +
        " historyRecords  WHERE historyRecords.id=:id")
    int delete(@Param("id") Long id);
}
