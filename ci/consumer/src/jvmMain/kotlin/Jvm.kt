// The two JVM-only modules. They are in maintenance, not withdrawn -- the README says they are
// still published, and this is where that claim is checked.

import io.github.youndie.telek.telegram.TelegramContextSource
import io.github.youndie.telek.telegram.effect.telegramEffectExecutor

@Suppress("unused")
private val jvmOnlyModulesResolve = {
    val source = TelegramContextSource()
    telegramEffectExecutor(source)
}
