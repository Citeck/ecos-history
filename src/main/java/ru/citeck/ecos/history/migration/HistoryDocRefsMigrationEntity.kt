package ru.citeck.ecos.history.migration

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "HISTORY_RECORDS")
class HistoryDocRefsMigrationEntity : Serializable {

    @Id
    var id: Long = -1L

    var documentId: String? = null
    var documentRefId: Long? = null
}
