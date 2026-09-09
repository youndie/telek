package io.github.youndie.telek.ktg

import dev.inmo.tgbotapi.bot.TelegramBot
import io.github.youndie.telek.ExecutionContext

public class KtgContext(
    public val bot: TelegramBot,
) : ExecutionContext
