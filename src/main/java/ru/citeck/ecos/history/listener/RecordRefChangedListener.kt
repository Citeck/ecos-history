package ru.citeck.ecos.history.listener

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.data.sql.records.refs.DbRecordRefService
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordRefChangedEvent
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Normally, ecos-data automatically updates the {@code ed_record_ref} table
 * using the {@code ed_associations} table to detect external webapps
 * that have source associations.
 *
 * However, history records are not stored via ecos-data.
 * Because of this, the automatic update does not apply to them,
 * and the migration must be performed manually.
 */
@Component
class RecordRefChangedListener(
    private val eventsService: EventsService,
    private val recordRefService: DbRecordRefService
) {

    @PostConstruct
    fun init() {
        eventsService.addListener {
            withEventType(RecordRefChangedEvent.TYPE )
            withDataClass(RefChangedData::class.java)
            withTransactional(true)
            withAction { event ->
                val userRef = EntityRef.create(AppName.EMODEL, "person", event.user)
                val userRefId = recordRefService.getOrCreateIdByEntityRef(userRef)
                recordRefService.migrateRefIfExists(event.before, event.after, userRefId)
            }
        }
    }

    class RefChangedData(
        val before: EntityRef,
        val after: EntityRef,
        @param:AttName($$"$event.user")
        val user: String
    )
}
