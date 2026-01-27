package ru.citeck.ecos.history.migration

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.EntityTransaction
import org.hibernate.SessionFactory
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.data.sql.context.DbSchemaContext
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.history.service.HistoryRecordServiceImpl
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosLocalPatch
import java.util.concurrent.Callable

/**
 * Migrate string representation of document ref
 * like 'emodel/document@c9a6f216-37dc-4c7e-bebe-59dd5e90a714'
 * to numeric id based on table public.ed_record_ref from ecos-data
 */
@Component
@EcosLocalPatch("history-doc-refs-migration", "2026-01-26T00:00:00Z", afterStart = true)
class HistoryDocRefsMigration(
    private val dbDomainFactory: DbDomainFactory,
    private val entityManagerFactory: EntityManagerFactory
) : Callable<DataValue> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun call(): DataValue {

        log.info { "+++ Begin History Documents References Migration +++" }

        val publicSchemaCtx: DbSchemaContext = dbDomainFactory.getSchemaContext("public")
        val recordRefService = publicSchemaCtx.recordRefService
        TxnContext.doInNewTxn {
            recordRefService.createTableIfNotExists()
        }

        fun processBatch(batch: List<HistoryDocRefsMigrationEntity>) {

            if (batch.isEmpty()) return

            val refs = batch.map {
                HistoryRecordServiceImpl.normalizeDocRef(it.documentId)
            }
            val refToIdMap = TxnContext.doInTxn {
                recordRefService.getOrCreateIdByEntityRefsMap(
                    refs.filterTo(HashSet()) { it.isNotEmpty() }
                )
            }
            for ((idx, entity) in batch.withIndex()) {
                entity.documentRefId = refToIdMap[refs[idx]] ?: 0
            }
        }

        var processed = 0
        var reportCounter = 0

        while (true) {

            val entityManager = entityManagerFactory.createEntityManager()
            val transaction: EntityTransaction = entityManager.transaction

            try {
                transaction.begin()

                val batchToProc = entityManager.createQuery(
                    "FROM HistoryDocRefsMigrationEntity WHERE documentRefId = -1 OR documentRefId IS NULL",
                    HistoryDocRefsMigrationEntity::class.java
                ).setMaxResults(100)
                    .getResultList()

                if (batchToProc.isEmpty()) {
                    break
                }
                TxnContext.doInNewTxn {
                    processBatch(batchToProc)
                }

                processed += batchToProc.size

                if ((processed / 10_000) > reportCounter) {
                    reportCounter += 1
                    log.info { "Processed $processed records" }
                }

                entityManager.flush()
                transaction.commit()
            } catch (e: Exception) {
                if (transaction.isActive) {
                    transaction.rollback()
                }
                throw e
            } finally {
                entityManager.close()
            }
        }

        entityManagerFactory.unwrap(SessionFactory::class.java).cache.evictAllRegions()

        log.info { "+++ History Documents References Migration Completed. Processed: $processed +++" }

        return DataValue.createObj()
            .set("processed", processed)
    }
}
