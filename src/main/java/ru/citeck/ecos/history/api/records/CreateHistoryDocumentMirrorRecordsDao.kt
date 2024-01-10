package ru.citeck.ecos.history.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.history.service.HistoryRecordService
import ru.citeck.ecos.records3.record.dao.mutate.ValueMutateDao
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class CreateHistoryDocumentMirrorRecordsDao(
    private val historyRecordService: HistoryRecordService
) : ValueMutateDao<CreateHistoryDocumentMirrorRecordsDao.MutateAtts> {

    override fun mutate(value: MutateAtts): String {
        historyRecordService.createHistoryDocumentMirror(value.documentMirrorRef, value.documentRef)
        return "OK"
    }

    override fun getId(): String {
        return "create-history-document-mirror"
    }

    class MutateAtts(
        val documentMirrorRef: EntityRef,
        val documentRef: EntityRef
    )
}
