package ru.citeck.ecos.history.service.impl

import org.apache.commons.lang3.time.FastDateFormat
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.data.sql.records.DbRecordsUtils
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.events2.type.RecordStatusChangedEvent
import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.service.HistoryEventType
import ru.citeck.ecos.history.service.HistoryRecordService
import ru.citeck.ecos.model.lib.attributes.dto.AttributeDef
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.type.dto.AssocDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.time.Instant
import java.time.ZoneId
import java.util.*
import javax.annotation.PostConstruct

@Component
class EcosEventsListener(
    private val eventsService: EventsService,
    private val historyRecordService: HistoryRecordService,
    private val typesRegistry: EcosTypesRegistry
) {

    companion object {

        private val DATE_FORMAT = FastDateFormat.getInstance(
            "dd.MM.yyyy",
            TimeZone.getTimeZone(ZoneId.of("UTC"))
        )

        private const val EMPTY_VALUE_STR = "—"

        private val LOCALES = listOf(
            I18nContext.RUSSIAN,
            I18nContext.ENGLISH
        )

        private val ADD_ACTION_TITLE = MLText(
            I18nContext.RUSSIAN to "добавлено",
            I18nContext.ENGLISH to "added"
        )

        private val REMOVE_ACTION_TITLE = MLText(
            I18nContext.RUSSIAN to "удалено",
            I18nContext.ENGLISH to "removed"
        )

        private val HISTORY_CONFIG_REF = EntityRef.create(AppName.EMODEL, "aspect", "history-config")
        private const val EXCLUDED_ATTS_ATT = "excludedAtts";
    }

    @PostConstruct
    fun init() {

        val buildStatusMsg = { statusBefore: StatusValue, statusAfter: StatusValue ->
            MLText(
                *LOCALES.map { locale ->
                    locale to statusBefore.name.getClosest(locale).ifBlank { "—" } +
                        " -> " + statusAfter.name.getClosest(locale)
                }.toTypedArray()
            ).toString()
        }

        val excludeBpmnElementsOptimizationPredicate = Predicates.notEq(
            "record._type?id",
            "${AppName.EMODEL}/type@bpmn-process-element"
        )

        eventsService.addListener<StatusChanged> {
            withEventType(RecordStatusChangedEvent.TYPE)
            withDataClass(StatusChanged::class.java)
            withFilter(excludeBpmnElementsOptimizationPredicate)
            withAction { event ->

                val record = hashMapOf<String, String>()

                record[HistoryRecordService.DOCUMENT_ID] = event.record.toString()
                record[HistoryRecordService.DOC_STATUS_NAME] = event.after.id
                record[HistoryRecordService.DOC_STATUS_TITLE] = event.after.name.getClosest(I18nContext.RUSSIAN)
                record[HistoryRecordService.EVENT_TYPE] = HistoryEventType.STATUS_CHANGED.value
                record[HistoryRecordService.USER_ID] = event.user
                record[HistoryRecordService.USERNAME] = event.user
                record[HistoryRecordService.CREATION_TIME] = formatTime(event.time)

                record[HistoryRecordService.COMMENTS] = buildStatusMsg(event.before, event.after)

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener<RecordCreated> {
            withEventType(RecordCreatedEvent.TYPE)
            withDataClass(RecordCreated::class.java)
            withFilter(excludeBpmnElementsOptimizationPredicate)
            withAction { event ->

                val record = hashMapOf<String, String>()

                record[HistoryRecordService.DOCUMENT_ID] = event.record.toString()
                record[HistoryRecordService.EVENT_TYPE] = HistoryEventType.NODE_CREATED.value
                record[HistoryRecordService.USER_ID] = event.user
                record[HistoryRecordService.USERNAME] = event.user
                record[HistoryRecordService.CREATION_TIME] = formatTime(event.time)

                val recordTypeDef = typesRegistry.getValue(event.recordTypeId)
                val typeAssocsId = recordTypeDef?.associations?.map { it.id }

                val assocsList = event.assocs ?: emptyList()
                val assocsId = event.assocs?.map { it.assocId } ?: emptyList()

                if (typeAssocsId?.any { it in assocsId } == true) {
                    for (assoc in assocsList) {
                        val typeAssocsById = recordTypeDef.associations.associateBy { it.id }
                        val typeAssoc = typeAssocsById[assoc.assocId]

                        if (typeAssoc != null &&
                            (
                                typeAssoc.direction == AssocDef.Direction.BOTH ||
                                    typeAssoc.direction == AssocDef.Direction.TARGET
                                )
                        ) {
                            assoc.added.forEach {
                                storeSourceAssocHistoryEvent(
                                    event.user,
                                    event.time,
                                    event.recordDispML,
                                    it,
                                    typeAssoc,
                                    true
                                )
                            }
                        }
                    }
                }

                historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
            }
        }

        eventsService.addListener<RecordUpdated> {
            withEventType(RecordChangedEvent.TYPE)
            withDataClass(RecordUpdated::class.java)
            withFilter(excludeBpmnElementsOptimizationPredicate)
            withAction { event ->

                val record = hashMapOf<String, String>()

                record[HistoryRecordService.DOCUMENT_ID] = event.record.toString()
                record[HistoryRecordService.EVENT_TYPE] = HistoryEventType.NODE_UPDATED.value
                record[HistoryRecordService.USER_ID] = event.user
                record[HistoryRecordService.USERNAME] = event.user
                record[HistoryRecordService.CREATION_TIME] = formatTime(event.time)
                if (event.version != null) {
                    record[HistoryRecordService.VERSION] = event.version
                }

                val attsById = event.typeAtts.associateBy { it.id }

                fun processChangedValue(changed: ChangedValue, allowAssocs: Boolean) {
                    val attDef = attsById[changed.attId] ?: return
                    if (!allowAssocs && DbRecordsUtils.isAssocLikeAttribute(attDef)) {
                        return
                    }
                    val comments = getCommentsForChangedValue(changed, attDef)
                    for (comment in comments) {
                        record[HistoryRecordService.COMMENTS] = Jsoup.parse(comment).text()
                        historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
                    }
                }

                val recordTypeDef = typesRegistry.getValue(event.recordTypeId)

                val historyConfig = recordTypeDef?.aspects?.first { HISTORY_CONFIG_REF == it.ref }?.config ?: ObjectData.create()
                val excludedAtts = historyConfig[EXCLUDED_ATTS_ATT].asStrList()
                event.changed.forEach {
                    if (!excludedAtts.contains(it.attId)) {
                        processChangedValue(it, false)
                    }
                }

                val typeAssocsById = recordTypeDef?.associations?.associateBy { it.id } ?: emptyMap()

                for (assoc in event.assocs) {
                    val attDef = attsById[assoc.assocId] ?: continue
                    if (excludedAtts.contains(attDef.id)) {
                        continue
                    }

                    val addedDisp = assoc.added.map { it.displayName }
                    val removedDisp = assoc.removed.map { it.displayName }
                    if (!attDef.multiple) {
                        processChangedValue(ChangedValue(assoc.assocId, removedDisp, addedDisp), true)
                    } else {

                        val fieldName = LOCALES.associateWith { attDef.name.getClosest(it) }

                        if (addedDisp.isNotEmpty()) {
                            val comment = MLText(
                                fieldName.mapValues {
                                    "${it.value}: ${ADD_ACTION_TITLE.getClosestValue(it.key)} " +
                                        addedDisp.joinToString(", ")
                                }
                            ).toString()

                            record[HistoryRecordService.COMMENTS] = Jsoup.parse(comment).text()
                            historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
                        }
                        if (removedDisp.isNotEmpty()) {
                            val comment = MLText(
                                fieldName.mapValues {
                                    "${it.value}: ${REMOVE_ACTION_TITLE.getClosestValue(it.key)} " +
                                        addedDisp.joinToString(", ")
                                }
                            ).toString()

                            record[HistoryRecordService.COMMENTS] = Jsoup.parse(comment).text()
                            historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
                        }
                    }
                    val typeAssoc = typeAssocsById[assoc.assocId]
                    if (typeAssoc != null &&
                        (
                            typeAssoc.direction == AssocDef.Direction.BOTH ||
                                typeAssoc.direction == AssocDef.Direction.TARGET
                            )
                    ) {
                        assoc.added.forEach {
                            storeSourceAssocHistoryEvent(
                                event.user,
                                event.time,
                                event.recordDispML,
                                it,
                                typeAssoc,
                                true
                            )
                        }
                        assoc.removed.forEach {
                            storeSourceAssocHistoryEvent(
                                event.user,
                                event.time,
                                event.recordDispML,
                                it,
                                typeAssoc,
                                false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun storeSourceAssocHistoryEvent(
        user: String,
        time: Instant,
        recordDispML: MLText,
        target: AssocTargetAtts,
        assoc: AssocDef,
        added: Boolean
    ) {
        val targetTypeDef = typesRegistry.getValue(target.typeId) ?: return
        val targetAssoc = targetTypeDef.associations.find { it.id == assoc.id } ?: return
        if (targetAssoc.direction != AssocDef.Direction.SOURCE && targetAssoc.direction != AssocDef.Direction.BOTH) {
            return
        }

        val record = hashMapOf<String, String>()

        record[HistoryRecordService.DOCUMENT_ID] = target.recordRef.toString()
        record[HistoryRecordService.EVENT_TYPE] = HistoryEventType.NODE_UPDATED.value
        record[HistoryRecordService.USER_ID] = user
        record[HistoryRecordService.USERNAME] = user
        record[HistoryRecordService.CREATION_TIME] = formatTime(time)
        if (target.version != null) {
            record[HistoryRecordService.VERSION] = target.version
        }

        val assocNames = LOCALES.associateWith { targetAssoc.name.getClosestValue(it) }

        val comment = MLText(
            assocNames.mapValues {
                "${it.value}: " +
                    if (added) {
                        ADD_ACTION_TITLE.getClosestValue(it.key)
                    } else {
                        REMOVE_ACTION_TITLE.getClosestValue(it.key)
                    } +
                    " ${recordDispML.getClosestValue(it.key)}"
            }
        ).toString()

        record[HistoryRecordService.COMMENTS] = comment

        historyRecordService.saveOrUpdateRecord(HistoryRecordEntity(), record)
    }

    private fun getCommentsForChangedValue(changed: ChangedValue, attDef: AttributeDef): List<String> {

        val fieldNames = LOCALES.associateWith { attDef.name.getClosest(it) }

        if (attDef.type == AttributeType.JSON) {

            val isEmptyBefore = isEmpty(changed.before)
            val isEmptyAfter = isEmpty(changed.after)

            if (isEmptyBefore && isEmptyBefore != isEmptyAfter) {

                val msgTemplate = fieldNames.mapValues {
                    "${it.value}: " + if (isEmptyBefore) {
                        ADD_ACTION_TITLE.getClosestValue()
                    } else {
                        REMOVE_ACTION_TITLE.getClosestValue()
                    }
                }

                val msg = MLText(msgTemplate).toString()

                return listOf(msg)
            } else if (isEmptyBefore && isEmptyAfter) {
                return emptyList()
            }

            val beforeStrArr = changed.before ?: emptyList()
            val afterStrArr = changed.after ?: emptyList()

            for (i in 0 until beforeStrArr.size.coerceAtLeast(afterStrArr.size)) {

                val beforeStrValue: String = beforeStrArr.getOrNull(i) ?: "{}"
                val afterStrValue: String = afterStrArr.getOrNull(i) ?: "{}"

                val valueBefore = convertArraysToObjects(DataValue.create(beforeStrValue))
                val valueAfter = convertArraysToObjects(DataValue.create(afterStrValue))

                val comments = arrayListOf<String>()
                collectJsonChangedEvents(fieldNames, "", valueBefore, valueAfter, comments)
                return comments
            }
        }

        val valueToStr: (List<String>?, AttributeType) -> String = { value, type ->

            val notEmptyValues: List<String> = if (value.isNullOrEmpty()) {
                emptyList()
            } else {
                value.filter { it.isNotBlank() }
            }

            if (notEmptyValues.isEmpty()) {
                EMPTY_VALUE_STR
            } else {
                notEmptyValues.joinToString(", ") {
                    when (type) {
                        AttributeType.DATETIME -> formatTime(Instant.parse(it))
                        AttributeType.DATE -> DATE_FORMAT.format(Date.from(Instant.parse(it)))
                        else -> it
                    }
                }
            }
        }

        val commentTemplate = MLText(
            fieldNames.mapValues {
                "${it.value}: ${valueToStr(changed.before, attDef.type)} -> ${valueToStr(changed.after, attDef.type)}"
            }
        ).toString()

        val comment = Jsoup.parse(commentTemplate).text()

        return listOf(comment)
    }

    private fun isEmpty(value: Any?): Boolean {
        return value == null ||
            value is Collection<*> && (value.isEmpty() || value.all { isEmpty(it) }) ||
            value is Map<*, *> && value.isEmpty() ||
            value is String && value.isEmpty() ||
            value is DataValue && (
            value.isNull() ||
                ((value.isArray() || value.isObject()) && value.size() == 0) ||
                value.isTextual() && value.asText().isEmpty()
            )
    }

    private fun convertArraysToObjects(value: DataValue): DataValue {
        return if (value.isArray()) {
            val data = DataValue.createObj()
            for (element in value) {
                val id = element["id"].asText().ifEmpty {
                    element["key"].asText()
                }
                if (id.isNotBlank()) {
                    data[id] = convertArraysToObjects(element)
                }
            }
            if (data.size() != value.size()) {
                value
            } else {
                data
            }
        } else if (value.isObject()) {
            val data = DataValue.createObj()
            value.forEach { k, v ->
                data[k] = convertArraysToObjects(v)
            }
            data
        } else {
            value
        }
    }

    private fun collectJsonChangedEvents(
        fieldNames: Map<Locale, String>,
        path: String,
        before: DataValue,
        after: DataValue,
        events: MutableList<String>
    ) {
        if (after.isObject()) {
            val keys = before.fieldNamesList().toMutableSet()
            keys.addAll(after.fieldNamesList())
            val added = mutableMapOf<String, DataValue>()
            val removed = mutableListOf<String>()
            for (key in keys) {
                if (!before.has(key)) {
                    added[key] = after[key]
                    continue
                } else if (!after.has(key)) {
                    removed.add(key)
                    continue
                }
                collectJsonChangedEvents(fieldNames, "$path.$key", before[key], after[key], events)
            }
            if (removed.isNotEmpty()) {
                val removedEvent = MLText(
                    fieldNames.mapValues {
                        "${it.value}$path ${REMOVE_ACTION_TITLE.getClosestValue()}: $removed"
                    }
                ).toString()

                events.add(Jsoup.parse(removedEvent).text())
            }
            if (added.isNotEmpty()) {
                added.forEach { (key, value) ->
                    val addedEvent = MLText(
                        fieldNames.mapValues {
                            "${it.value}$path ${ADD_ACTION_TITLE.getClosestValue()}: $key = $value"
                        }
                    ).toString()

                    events.add(Jsoup.parse(addedEvent).text())
                }
            }
        } else if (before != after) {
            val event = MLText(
                fieldNames.mapValues {
                    "${it.value}$path: $before -> $after}"
                }
            ).toString()

            events.add(Jsoup.parse(event).text())
        }
    }

    private fun formatTime(time: Instant): String {
        return HistoryRecordServiceImpl.dateFormat.format(Date.from(time))
    }

    data class RecordUpdated(
        @AttName("record.version:version")
        val version: String?,
        @AttName("record?id")
        val record: RecordRef,
        @AttName("record._type?localId!")
        val recordTypeId: String,
        @AttName("record._disp?json")
        val recordDispML: MLText,
        @AttName("\$event.time")
        val time: Instant,
        @AttName("\$event.user")
        val user: String,
        @AttName("diff.list[]")
        val changed: List<ChangedValue>,
        val assocs: List<AssocDiff>,
        @AttName("typeDef.model.attributes[]?json")
        val typeAtts: List<AttributeDef>
    )

    data class AssocDiff(
        val assocId: String,
        val child: Boolean,
        val added: List<AssocTargetAtts>,
        val removed: List<AssocTargetAtts>
    )

    data class AssocTargetAtts(
        @AttName("_type?localId!")
        val typeId: String,
        @AttName("?disp!_type?disp!?localId")
        val displayName: String,
        @AttName("?id")
        val recordRef: EntityRef,
        @AttName("version:version")
        val version: String?,
        @AttName("_disp?json")
        val dispML: MLText
    )

    data class ChangedValue(
        @AttName("def.id")
        val attId: String,
        @AttName("before[]?disp")
        val before: List<String>?,
        @AttName("after[]?disp")
        val after: List<String>?
    )

    data class RecordCreated(
        @AttName("record?id")
        val record: RecordRef,
        @AttName("\$event.time")
        val time: Instant,
        @AttName("\$event.user")
        val user: String,
        val isDraft: Boolean,
        val assocs: List<AssocInfo>?,
        @AttName("record._type?localId!")
        val recordTypeId: String,
        @AttName("record._disp?json")
        val recordDispML: MLText
    )

    data class AssocInfo(
        val added: List<AssocTargetAtts>,
        val assocId: String,
        @AttName("def.name?json")
        val name: MLText
    )

    data class StatusChanged(
        @AttName("record?id")
        val record: RecordRef,
        val before: StatusValue,
        val after: StatusValue,
        @AttName("\$event.time")
        val time: Instant,
        @AttName("\$event.user")
        val user: String
    )

    data class StatusValue(
        val id: String,
        val name: MLText
    )
}
