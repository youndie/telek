package ru.workinprogress.telek.telegram

import com.github.kotlintelegrambot.Bot
import ru.workinprogress.telek.ExecutionContext

class TelegramContext(
    val bot: Bot,
) : ExecutionContext
