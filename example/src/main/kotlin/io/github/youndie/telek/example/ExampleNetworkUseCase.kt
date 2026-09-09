package io.github.youndie.telek.example

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class ExampleNetworkUseCase(
    private val httpClient: HttpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                    },
                )
            }
        },
) {
    // The example a reader copies, so it shows the shape rather than the shortcut: `runCatching`
    // here would have handed the caller a cancelled request as `Result.failure`, and the screen
    // that navigated away would have drawn it as a network error.
    suspend operator fun invoke(): Result<CatFact> =
        try {
            // simulate long request
            delay(1000)
            Result.success(httpClient.get("https://catfact.ninja/fact").body<CatFact>())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
}

@Serializable
data class CatFact(
    val fact: String,
    val length: Int,
)
