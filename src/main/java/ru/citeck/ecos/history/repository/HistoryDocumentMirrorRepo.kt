package ru.citeck.ecos.history.repository

import org.springframework.data.repository.CrudRepository
import ru.citeck.ecos.history.domain.HistoryDocumentMirrorEntity

interface HistoryDocumentMirrorRepo : CrudRepository<HistoryDocumentMirrorEntity, Long> {

    fun findAllByDocumentMirrorRef(documentMirrorRef: Long): List<HistoryDocumentMirrorEntity>

    fun findByDocumentMirrorRefAndDocumentRef(documentMirrorRef: Long, documentRef: Long): HistoryDocumentMirrorEntity?
}
