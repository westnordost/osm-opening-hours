
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.Path

open class UpdateOpeningHoursTask : DefaultTask() {

    @get:Input lateinit var targetFiles: List<String>

    @TaskAction fun run() {

        val query = """
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

        val client = HttpClient.newHttpClient()
        val requestBody = "action=tsv_export&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://qlever.dev/api/osm-planet"))
            .header("User-Agent", "osm-opening-hours")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(BodyPublishers.ofString(requestBody))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body()

        for (targetFile in targetFiles) {
            val path = Path(project.projectDir.toString(), targetFile)
            Files.write(path, response)
        }
    }
}
