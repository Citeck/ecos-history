package ru.citeck.ecos.history.domain

import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name = "history_document_mirror")
class HistoryDocumentMirrorEntity : Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", sequenceName = "hibernate_sequence")
    @Column(name = "id", updatable = false, nullable = false)
    var id: Long? = null

    var documentMirrorRef: Long = -1L
    var documentRef: Long = -1L
}
