package io.github.youndie.telek.telegram

import com.github.kotlintelegrambot.Bot
import io.github.youndie.telek.ExecutionContext

public class TelegramContext(
    public val bot: Bot,
) : ExecutionContext
