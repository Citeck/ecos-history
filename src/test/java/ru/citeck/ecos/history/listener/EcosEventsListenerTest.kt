package ru.citeck.ecos.history.listener

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.listener.ListenerConfig
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.history.service.HistoryRecordService
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.time.Instant
import java.util.Locale

class EcosEventsListenerTest {

    private val ru = Locale.forLanguageTag("ru")

    @Test
    fun `removal-logs-the-names-of-the-removed-attachments`() {
        val comments = associationChange(removed = listOf("Invoice.xml", "Acceptance.pdf"))

        assertThat(comments).containsExactly(
            "Attachments: removed Invoice.xml, Acceptance.pdf" to "Вложения: удалено Invoice.xml, Acceptance.pdf"
        )
    }

    @Test
    fun `replacement-logs-added-and-removed-attachments-separately`() {
        val comments = associationChange(added = listOf("New.pdf"), removed = listOf("Old.pdf"))

        assertThat(comments).containsExactly(
            "Attachments: added New.pdf" to "Вложения: добавлено New.pdf",
            "Attachments: removed Old.pdf" to "Вложения: удалено Old.pdf"
        )
    }

    @Test
    fun `addition-logs-only-an-addition`() {
        val comments = associationChange(added = listOf("Invoice.xml"))

        assertThat(comments).containsExactly("Attachments: added Invoice.xml" to "Вложения: добавлено Invoice.xml")
    }

    private fun associationChange(added: List<String> = emptyList(), removed: List<String> = emptyList()): List<Pair<String, String>> {
        val subscriptions = mutableListOf<ListenerConfig<Any>>()
        val events = Mockito.mock(EventsService::class.java) { call ->
            if (call.method.name == "addListener") {
                subscriptions += ListenerConfig.create(call.getArgument<ListenerConfig.Builder<Any>.() -> Unit>(0))
            }
            null
        }
        val comments = mutableListOf<Pair<String, String>>()
        val history = Mockito.mock(HistoryRecordService::class.java) { call ->
            if (call.method.name == "saveOrUpdateRecord" && call.arguments.size == 2) {
                val comment = call.getArgument<Map<String, String>>(1).getValue(HistoryRecordService.COMMENTS)
                val localized = requireNotNull(Json.mapper.read(comment, MLText::class.java))
                comments += localized.getClosest(Locale.ENGLISH) to localized.getClosest(ru)
            }
            null
        }
        EcosEventsListener(events, history, Mockito.mock(EcosTypesRegistry::class.java)).init()
        val update = subscriptions.single { it.eventType == RecordChangedEvent.TYPE && it.dataClass == EcosEventsListener.RecordUpdated::class.java }
        update.action.accept(
            EcosEventsListener.RecordUpdated(
                version = null,
                record = EntityRef.valueOf("emodel/test-request@1"),
                recordTypeId = "test-request",
                recordDispML = MLText("Request"),
                time = Instant.parse("2026-11-20T10:00:00Z"),
                user = "test-user",
                changed = emptyList(),
                assocs = listOf(EcosEventsListener.AssocDiff("attachments", true, added.map(::attachment), removed.map(::attachment))),
                typeAtts = listOf(AttributeDef.create {
                    withId("attachments")
                    withType(AttributeType.ASSOC)
                    withMultiple(true)
                    withName(MLText(Locale.ENGLISH to "Attachments", ru to "Вложения"))
                })
            )
        )
        return comments
    }

    private fun attachment(name: String) = EcosEventsListener.AssocTargetAtts(
        typeId = "test-attachment",
        displayName = name,
        recordRef = EntityRef.valueOf("emodel/test-attachment@$name"),
        version = null,
        dispML = MLText(name)
    )
}
