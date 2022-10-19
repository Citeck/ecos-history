package ru.citeck.ecos.history.service.task.impl

import org.apache.commons.lang3.LocaleUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.emitter.EmitterConfig
import ru.citeck.ecos.events2.emitter.EventsEmitter
import ru.citeck.ecos.history.HistoryApp
import ru.citeck.ecos.history.domain.HistoryRecordEntity
import ru.citeck.ecos.history.dto.TaskRole
import ru.citeck.ecos.history.repository.HistoryRecordRepository
import ru.citeck.ecos.history.service.HistoryEventType
import ru.citeck.ecos.history.service.impl.*
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [HistoryApp::class])
class BpmnTaskEventHistoryTest {

    @Value("\${spring.application.name}")
    private lateinit var appName: String

    @Autowired
    private lateinit var eventsService: EventsService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var historyRecordRepository: HistoryRecordRepository

    private lateinit var userTaskCreateEmitter: EventsEmitter<UserTaskEvent>
    private lateinit var userTaskCompleteEmitter: EventsEmitter<UserTaskEvent>
    private lateinit var userTaskAssignEmitter: EventsEmitter<UserTaskEvent>

    companion object {
        private val documentRecord = DocumentRecord()
        private val documentRef = RecordRef.valueOf("data/doc@1")

        private val taskRef = RecordRef.valueOf("proc/task@1")
        private val procInstanceRef = RecordRef.valueOf("proc/procInstaceId@1")
        private val formRef = RecordRef.valueOf("ui/form@task-form")
        private val taskName = MLText(
            LocaleUtils.toLocale("en") to "Task name",
            LocaleUtils.toLocale("ru") to "Название задачи"
        )

        private const val runAsUser = "ivan"
        private const val outcome = "done"
        private val outcomeName = MLText(
            LocaleUtils.toLocale("en") to "Done",
            LocaleUtils.toLocale("ru") to "Выполнено"
        )
        private const val comment = "Task is done!"
        private val roles = listOf(
            TaskRole(
                "role1",
                MLText(
                    LocaleUtils.toLocale("en") to "Role 1",
                    LocaleUtils.toLocale("ru") to "Роль 1"
                )
            ),
            TaskRole(
                "role2",
                MLText(
                    LocaleUtils.toLocale("en") to "Role 2",
                    LocaleUtils.toLocale("ru") to "Роль 2"
                )
            )
        )
    }

    @BeforeEach
    fun setUp() {
        recordsService.register(
            RecordsDaoBuilder.create("data/doc")
                .addRecord(
                    documentRef.id,
                    documentRecord
                )
                .build()
        )

        userTaskCreateEmitter = eventsService.getEmitter(
            EmitterConfig.create<UserTaskEvent> {
                source = appName
                eventType = BPMN_EVENT_USER_TASK_CREATE
                eventClass = UserTaskEvent::class.java
            }
        )

        userTaskCompleteEmitter = eventsService.getEmitter(
            EmitterConfig.create<UserTaskEvent> {
                source = appName
                eventType = BPMN_EVENT_USER_TASK_COMPLETE
                eventClass = UserTaskEvent::class.java
            }
        )

        userTaskAssignEmitter = eventsService.getEmitter(
            EmitterConfig.create<UserTaskEvent> {
                source = appName
                eventType = BPMN_EVENT_USER_TASK_ASSIGN
                eventClass = UserTaskEvent::class.java
            }
        )
    }

    @Test
    fun `task event history count check`() {

        userTaskCreateEmitter.emit(
            UserTaskEvent(
                taskId = taskRef,
                procInstanceId = procInstanceRef,
                name = taskName,
                document = documentRef,
                form = formRef
            )
        )

        userTaskCreateEmitter.emit(
            UserTaskEvent(
                taskId = taskRef,
                procInstanceId = procInstanceRef,
                name = taskName,
                document = documentRef,
                form = formRef
            )
        )

        val allHistory = historyRecordRepository.findAll()

        assertThat(allHistory).hasSize(2)
    }

    @Test
    fun `task create event history payload check`() {

        val event = UserTaskEvent(
            taskId = taskRef,
            procInstanceId = procInstanceRef,
            name = taskName,
            document = documentRef,
            form = formRef
        )

        AuthContext.runAs(runAsUser) {
            userTaskCreateEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        compareCommonPayload(event, actualEntity, HistoryEventType.TASK_CREATED.value)

        assertThat(actualEntity.username).isEqualTo(runAsUser)
    }

    @Test
    fun `task complete event history payload check`() {

        val event = UserTaskEvent(
            taskId = taskRef,
            procInstanceId = procInstanceRef,
            name = taskName,
            document = documentRef,
            form = formRef,
            outcome = outcome,
            outcomeName = outcomeName,
            roles = roles,
            comment = comment
        )

        AuthContext.runAs(runAsUser) {
            userTaskCompleteEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        compareCommonPayload(event, actualEntity, HistoryEventType.TASK_COMPLETE.value)

        assertThat(actualEntity.username).isEqualTo(runAsUser)
        assertThat(actualEntity.taskOutcome).isEqualTo(outcome)
        assertThat(actualEntity.taskOutcomeName).isEqualTo(outcomeName.toString())
        assertThat(actualEntity.taskRole).isEqualTo(Json.mapper.toString(roles))
        assertThat(actualEntity.comments).isEqualTo(comment)
    }

    @Test
    fun `task assign event history payload check`() {

        val event = UserTaskEvent(
            taskId = taskRef,
            procInstanceId = procInstanceRef,
            name = taskName,
            document = documentRef,
            form = formRef,
            assignee = "petya"
        )

        AuthContext.runAs(runAsUser) {
            userTaskAssignEmitter.emit(event)
        }

        val actualEntity = historyRecordRepository.findAll().first()

        compareCommonPayload(event, actualEntity, HistoryEventType.TASK_ASSIGN.value)

        assertThat(actualEntity.username).isEqualTo(event.assignee)
    }

    private fun compareCommonPayload(expected: UserTaskEvent, actual: HistoryRecordEntity, type: String) {
        with(actual) {
            assertThat(historyEventId).isNotBlank
            assertThat(documentId).isEqualTo(expected.document.toString())
            assertThat(version).isEqualTo(documentRecord.version)
            assertThat(eventType).isEqualTo(type)
            assertThat(workflowInstanceId).isEqualTo(expected.procInstanceId.toString())
            assertThat(taskTitle).isEqualTo(expected.name.toString())
            assertThat(taskEventInstanceId).isEqualTo(expected.taskId.toString())
            assertThat(taskFormKey).isEqualTo(expected.form.toString())
            assertThat(docType).isEqualTo(documentRecord.type.toString())
            assertThat(userId).isEqualTo(runAsUser)
            assertThat(creationTime).isNotNull
        }
    }

    @AfterEach
    fun clean() {
        historyRecordRepository.deleteAll()
    }
}

class DocumentRecord(

    @AttName("version")
    val version: String = "1.0",

    @AttName("_type")
    val type: RecordRef = RecordRef.valueOf("tp/type@document")
)
