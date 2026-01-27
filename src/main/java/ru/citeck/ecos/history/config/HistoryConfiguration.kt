package ru.citeck.ecos.history.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.data.sql.domain.DbDomainFactory
import ru.citeck.ecos.data.sql.records.refs.DbRecordRefService

@Configuration
class HistoryConfiguration {

    @Bean
    fun publicRecordRefService(dbDomainFactory: DbDomainFactory): DbRecordRefService {
        return dbDomainFactory.getSchemaContext("public").recordRefService
    }
}
