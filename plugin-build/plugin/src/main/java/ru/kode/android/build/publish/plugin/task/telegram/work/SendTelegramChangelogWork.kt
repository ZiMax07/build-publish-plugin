package ru.kode.android.build.publish.plugin.task.telegram.work

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import ru.kode.android.build.publish.plugin.task.telegram.sender.TelegramWebhookSender
import java.net.URLEncoder
import javax.inject.Inject

interface SendTelegramChangelogParameters : WorkParameters {
    val baseOutputFileName: Property<String>
    val buildName: Property<String>
    val changelog: Property<String>
    val webhookUrl: Property<String>
    val userMentions: Property<String>
    val escapedCharacters: Property<String>
    val botId: Property<String>
    val chatsId: SetProperty<String>
}

abstract class SendTelegramChangelogWork @Inject constructor() : WorkAction<SendTelegramChangelogParameters> {

    private val logger = Logging.getLogger(this::class.java)
    private val webhookSender = TelegramWebhookSender(logger)

    override fun execute() {
        val baseOutputFileName = parameters.baseOutputFileName.get()
        val buildName = parameters.buildName.get()
        val tgUserMentions = parameters.userMentions.get()
        val message = buildString {
            append("*$baseOutputFileName $buildName*")
            appendLine()
            append(tgUserMentions)
            appendLine()
            appendLine()
            append(parameters.changelog.get())
        }.formatChangelog(parameters.escapedCharacters.get())

        parameters.chatsId.get().forEach { chatId ->
            val url = parameters.webhookUrl.get().format(
                parameters.botId.get(),
                chatId,
                URLEncoder.encode(message, "utf-8")
            )
            webhookSender.send(url)
            logger.debug("changelog sent to Telegram")
        }
    }
}

private fun String.formatChangelog(escapedCharacters: String): String {
    return this
        .replace(escapedCharacters.toRegex()) { result -> "\\${result.value}" }
        .replace(Regex("(\r\n|\n)"), "\n")
        .replace("[-]".toRegex()) { result -> "\\${result.value}" }
}
