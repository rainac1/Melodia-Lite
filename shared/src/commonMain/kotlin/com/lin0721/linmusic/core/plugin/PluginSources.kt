package com.lin0721.linmusic.core.plugin

import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.source.AudioSourceProvider
import com.lin0721.linmusic.core.source.ExternalTrack
import com.lin0721.linmusic.core.source.MusicPlatform
import com.lin0721.linmusic.core.source.SourceResult
import com.lin0721.linmusic.core.source.isTrialPlayUrl
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "PluginSources"
private const val RESOLVE_BUDGET_MS = 15_000L

// 交给插件音源的歌曲信息；origin 为 songId 所属平台
data class PluginTrackQuery(
    val songId: String,
    val title: String,
    val artists: String,
    val album: String,
    val durationMs: Long,
    val origin: MusicPlatform
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("songId", songId)
        put("title", title)
        put("artists", artists)
        put("album", album)
        put("durationMs", durationMs)
        put("origin", origin.key)
    }

    companion object {
        fun of(track: ExternalTrack): PluginTrackQuery =
            PluginTrackQuery(track.id, track.name, track.artists, track.albumName, track.durationMs, track.platform)
    }
}

// 插件音源：按插件名称顺序逐个尝试，参与 AudioSourceRouter 的社区源轮询
class PluginAudioSourceProvider(private val registry: ExtensionRegistry) : AudioSourceProvider {

    override val platform: MusicPlatform = MusicPlatform.LX

    override suspend fun resolveUrl(
        songId: String,
        songName: String,
        artists: String,
        albumName: String?,
        durationMs: Long,
        quality: String,
        origin: MusicPlatform
    ): SourceResult? {
        val sources = registry.get(ExtensionPoints.AUDIO_SOURCE)
        if (sources.isEmpty()) return null
        val query = PluginTrackQuery(songId, songName, artists, albumName.orEmpty(), durationMs, origin)
        // 单个音源最多等 8 秒，插件多时要有总上限，后面还有其他换源方式
        return withTimeoutOrNull(RESOLVE_BUDGET_MS) {
            for (source in sources) {
                val url = PluginResults.url(source.value.resolve(query, quality))
                when {
                    url == null -> continue
                    isTrialPlayUrl(url) -> AppLogger.w(TAG, "插件 [${source.pluginName}] 返回试听链接，继续尝试: $songName")
                    else -> {
                        AppLogger.i(TAG, "插件 [${source.pluginName}] 音源 ${source.localId} 解析成功: $songName")
                        return@withTimeoutOrNull SourceResult(url = url, platform = platform, quality = quality, pluginId = source.pluginId)
                    }
                }
            }
            null
        }
    }

    override suspend fun search(keyword: String, offset: Int, limit: Int) = emptyList<ExternalTrack>()
}

// 插件返回值的宽松解析：取不到就当没有结果
internal object PluginResults {

    // 字符串直接作为链接，或对象的 url 字段；只接受 http(s)
    fun url(result: JsonElement?): String? {
        val text = when (result) {
            is JsonPrimitive -> result.takeIf { it.isString }?.content
            is JsonObject -> (result["url"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            else -> null
        }?.trim()
        return text?.takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
    }

}
