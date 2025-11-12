package ru.citeck.ecos.history.service.task.impl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.emitter.EmitterConfig
import ru.citeck.ecos.events2.emitter.EventsEmitter
import ru.citeck.ecos.history.HistoryApp
import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.repository.HistoryRecordRepository
import ru.citeck.ecos.history.service.impl.*
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [HistoryApp::class])
class ProcDefEventHistoryTest {

    companion object {
        private const val VERSION = 3.0
        private const val USER_IVAN = "ivan"

        private val procDefRef = EntityRef.valueOf("proc/procDef@1")
    }

    @Value("\${spring.application.name}")
    private lateinit var appName: String

    @Autowired
    private lateinit var eventsService: EventsService

    @Autowired
    private lateinit var historyRecordRepository: HistoryRecordRepository

    private lateinit var procDefCreateEmitter: EventsEmitter<ProcDefEvent>
    private lateinit var procDefUpdateEmitter: EventsEmitter<ProcDefEvent>
    private lateinit var procDefDeployedEmitter: EventsEmitter<ProcDefEvent>

    @BeforeEach
    fun setUp() {
        procDefCreateEmitter = eventsService.getEmitter(
            EmitterConfig.create<ProcDefEvent> {
                source = appName
                eventType = PROC_DEF_EVENT_CREATE
                eventClass = ProcDefEvent::class.java
            }
        )

        procDefUpdateEmitter = eventsService.getEmitter(
            EmitterConfig.create<ProcDefEvent> {
                source = appName
                eventType = PROC_DEF_EVENT_UPDATE
                eventClass = ProcDefEvent::class.java
            }
        )

        procDefDeployedEmitter = eventsService.getEmitter(
            EmitterConfig.create<ProcDefEvent> {
                source = appName
                eventType = PROC_DEF_EVENT_DEPLOYED
                eventClass = ProcDefEvent::class.java
            }
        )
    }

    @Test
    fun `proc def create payload check`() {
        val event = ProcDefEvent(
            procDefRef = procDefRef,
            version = VERSION
        )

        emitEventAsUser(USER_IVAN) {
            procDefCreateEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        comparePayload(event, actualEntity)

        assertThat(actualEntity.comments).isEqualTo(
            MLText(
                I18nContext.RUSSIAN to "Описание процесса создано",
                I18nContext.ENGLISH to "Process definition is created"
            ).toString()
        )
    }

    @Test
    fun `proc def raw create payload check`() {
        val event = ProcDefEvent(
            procDefRef = procDefRef,
            version = VERSION,
            dataState = PROC_DEF_DATA_STATE_RAW
        )

        emitEventAsUser(USER_IVAN) {
            procDefCreateEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        comparePayload(event, actualEntity)

        assertThat(actualEntity.comments).isEqualTo(
            MLText(
                I18nContext.RUSSIAN to "Описание процесса создано (черновик)",
                I18nContext.ENGLISH to "Process definition is created (draft)"
            ).toString()
        )
    }

    @Test
    fun `proc def update payload check`() {
        val event = ProcDefEvent(
            procDefRef = procDefRef,
            version = VERSION
        )

        emitEventAsUser(USER_IVAN) {
            procDefUpdateEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        comparePayload(event, actualEntity)

        assertThat(actualEntity.comments).isEqualTo(
            MLText(
                I18nContext.RUSSIAN to "Версия обновлена -> $VERSION",
                I18nContext.ENGLISH to "Version is updated -> $VERSION"
            ).toString()
        )
    }

    @Test
    fun `proc def raw update payload check`() {
        val event = ProcDefEvent(
            procDefRef = procDefRef,
            version = VERSION,
            dataState = PROC_DEF_DATA_STATE_RAW
        )

        emitEventAsUser(USER_IVAN) {
            procDefUpdateEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        comparePayload(event, actualEntity)

        assertThat(actualEntity.comments).isEqualTo(
            MLText(
                I18nContext.RUSSIAN to "Версия обновлена -> $VERSION (черновик)",
                I18nContext.ENGLISH to "Version is updated -> $VERSION (draft)"
            ).toString()
        )
    }

    @Test
    fun `proc def update from version payload check`() {
        val event = ProcDefEvent(
            procDefRef = procDefRef,
            version = VERSION,
            createdFromVersion = 2.0
        )

        emitEventAsUser(USER_IVAN) {
            procDefUpdateEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        comparePayload(event, actualEntity)

        assertThat(actualEntity.comments).isEqualTo(
            MLText(
                I18nContext.RUSSIAN to "Версия обновлена ${event.createdFromVersion} -> $VERSION",
                I18nContext.ENGLISH to "Version is updated ${event.createdFromVersion} -> $VERSION"
            ).toString()
        )
    }

    @Test
    fun `proc def raw update from version payload check`() {
        val event = ProcDefEvent(
            procDefRef = procDefRef,
            version = VERSION,
            createdFromVersion = 2.0,
            dataState = PROC_DEF_DATA_STATE_RAW
        )

        emitEventAsUser(USER_IVAN) {
            procDefUpdateEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        comparePayload(event, actualEntity)

        assertThat(actualEntity.comments).isEqualTo(
            MLText(
                I18nContext.RUSSIAN to "Версия обновлена ${event.createdFromVersion} -> $VERSION (черновик)",
                I18nContext.ENGLISH to "Version is updated ${event.createdFromVersion} -> $VERSION (draft)"
            ).toString()
        )
    }

    @Test
    fun `proc def deployed payload check`() {
        val event = ProcDefEvent(
            procDefRef = procDefRef,
            version = VERSION
        )

        emitEventAsUser(USER_IVAN) {
            procDefDeployedEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        comparePayload(event, actualEntity)

        assertThat(actualEntity.comments).isEqualTo(PROC_DEF_VERSION_DEPLOYED_MSG.toString())
    }

    @AfterEach
    fun clean() {
        historyRecordRepository.deleteAll()
    }

    private fun comparePayload(expected: ProcDefEvent, actual: HistoryRecordEntity) {
        with(actual) {
            assertThat(historyEventId).isNotBlank
            assertThat(creationTime).isNotNull

            assertThat(procDefRef).isEqualTo(expected.procDefRef)
            assertThat(version!!.toDouble()).isEqualTo(expected.version)
            assertThat(userId).isEqualTo(USER_IVAN)
        }
    }
}
