package tasks.update_opening_hours

import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.FileSystem
import okio.Path.Companion.toPath

private const val targetFile = "./src/jvmTest/resources/opening_hours_counts.tsv"

private const val qleverBaseUrl = "https://qlever.cs.uni-freiburg.de/api/osm-planet"

private val qleverQuery = """
    PREFIX osmkey: <https://www.openstreetmap.org/wiki/Key:>
    SELECT ?times (COUNT(?times) as ?count) WHERE {
    {?osm_id osmkey:opening_hours ?times}
    UNION
    {?osm_id osmkey:collection_times ?times}
    UNION
    {?osm_id osmkey:service_times ?times}
    }
    GROUP BY ?times
    ORDER BY DESC(?count) DESC(?times)
""".trimIndent().replace("\n"," ").replace("\r","")

fun main() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(qleverBaseUrl)
        .header("User-Agent", "osm-opening-hours test")
        .post(FormBody.Builder()
            .add("action", "tsv_export")
            .add("query", qleverQuery)
            .build()
        )
        .build()
    val response = client.newCall(request).execute()

    FileSystem.SYSTEM.write(targetFile.toPath(), mustCreate = false) {
        writeAll(response.body.source())
    }
}