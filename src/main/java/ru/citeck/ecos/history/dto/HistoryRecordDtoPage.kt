package ru.citeck.ecos.history.dto

import lombok.Data

@Data
class HistoryRecordDtoPage {

    var historyRecordDtos: List<HistoryRecordDto>? = emptyList()
    var totalElementsCount: Long? = 0L
    var totalPages: Int? = 0

    constructor() {}
}
