package ru.citeck.ecos.history.api.records

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.history.repository.HistoryRecordRepository
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@SpringBootTest
@ExtendWith(EcosSpringExtension::class)
class HistoryRecordsDaoTest {

    @Autowired
    lateinit var records: RecordsService

    @Autowired
    lateinit var repo: HistoryRecordRepository

    @BeforeEach
    fun beforeEach() {
        repo.deleteAll()
    }

    @Test
    fun test() {

        val rec = records.create(
            "history-record",
            mapOf(
                "documentId" to "test/test@test",
                "creationTime" to System.currentTimeMillis(),
                "eventType" to "task.assigned",
                "userId" to "admin",
                "username" to "admin",
                "comments" to "comments"
            )
        )

        val query = RecordsQuery.create {
            withSourceId("history-record")
        }

        val recs0 = records.query(
            query.copy {
                withQuery(Predicates.`in`("eventType", listOf("task.assigned")))
            }
        ).getRecords()
        assertThat(recs0).hasSize(1)
        assertThat(recs0[0]).isEqualTo(rec)

        val recs1 = records.query(
            query.copy {
                withQuery(Predicates.`in`("eventType", listOf("unknown")))
            }
        ).getRecords()
        assertThat(recs1).isEmpty()
    }
}
