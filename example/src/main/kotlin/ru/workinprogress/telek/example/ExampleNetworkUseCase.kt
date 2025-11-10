package ru.workinprogress.telek.example

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    suspend operator fun invoke() =
        runCatching {
            // simulate long request
            delay(1000)
            httpClient.get("https://catfact.ninja/fact").body<CatFact>()
        }
}

@Serializable
data class CatFact(
    val fact: String,
    val length: Int,
)
