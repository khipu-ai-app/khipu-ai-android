package pe.khipuai.app.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import pe.khipuai.app.BuildConfig
import pe.khipuai.app.data.remote.KhipuApiService
import pe.khipuai.app.data.remote.dto.*
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType

@Singleton
class TutorRepository @Inject constructor(
    private val apiService: KhipuApiService,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    suspend fun createSession(title: String): Result<ChatSessionResponse> {
        return try {
            val req = ChatSessionCreateRequest(title)
            Result.success(apiService.createChatSession(req))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessions(): Result<List<ChatSessionResponse>> {
        return try {
            Result.success(apiService.getChatSessions())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(sessionId: String): Result<List<ChatMessageResponse>> {
        return try {
            Result.success(apiService.getChatMessages(sessionId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamChatMessages(sessionId: String, messageText: String, courseId: String?): Flow<TutorStreamEvent> = callbackFlow {
        val courseJsonStr = if (courseId != null) "\"$courseId\"" else "null"
        val requestBody = """
            {
                "message": "$messageText",
                "course_id": $courseJsonStr
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${BuildConfig.BASE_URL}v1/tutor/sessions/$sessionId/messages")
            .post(requestBody)
            .header("Accept", "text/event-stream")
            .build()

        val factory = EventSources.createFactory(okHttpClient)
        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val jsonObj = json.decodeFromString<JsonObject>(data)
                    if (jsonObj.containsKey("text")) {
                        val text = jsonObj["text"]?.jsonPrimitive?.content ?: ""
                        trySend(TutorStreamEvent.Chunk(text))
                    } else if (jsonObj.containsKey("done")) {
                        val refsJson = jsonObj["references"]?.jsonArray
                        val references = mutableListOf<KnowledgeRefDto>()
                        
                        refsJson?.forEach { element ->
                            val obj = element.jsonObject
                            val noteId = obj["note_id"]?.jsonPrimitive?.content ?: ""
                            val noteTitle = obj["note_title"]?.jsonPrimitive?.content ?: ""
                            val snippet = obj["snippet"]?.jsonPrimitive?.content ?: ""
                            references.add(KnowledgeRefDto(noteId, noteTitle, snippet))
                        }
                        
                        trySend(TutorStreamEvent.Done(references))
                        close()
                    } else if (jsonObj.containsKey("error")) {
                        val err = jsonObj["error"]?.jsonPrimitive?.content ?: "Error de servidor"
                        trySend(TutorStreamEvent.Error(err))
                        close()
                    }
                } catch (e: Exception) {
                    trySend(TutorStreamEvent.Chunk(data))
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                trySend(TutorStreamEvent.Error(t?.localizedMessage ?: "Fallo de conexión de red"))
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = factory.newEventSource(request, listener)
        awaitClose {
            eventSource.cancel()
        }
    }
}

data class KnowledgeRefDto(
    val noteId: String,
    val noteTitle: String,
    val snippet: String
)

sealed class TutorStreamEvent {
    data class Chunk(val text: String) : TutorStreamEvent()
    data class Done(val references: List<KnowledgeRefDto>) : TutorStreamEvent()
    data class Error(val message: String) : TutorStreamEvent()
}
