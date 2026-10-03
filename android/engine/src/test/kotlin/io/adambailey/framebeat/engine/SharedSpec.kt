package io.adambailey.framebeat.engine

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Loads the shared test cases in `spec/` at the repo root. The web and Swift
 * test suites run the same files, so all three copies of every constant are
 * checked against one contract. Gradle passes the directory in as
 * `framebeat.specDir` (see `engine/build.gradle.kts`).
 */
object SharedSpec {
    private val dir = File(
        checkNotNull(System.getProperty("framebeat.specDir")) { "framebeat.specDir is not set; run the tests through Gradle" },
    )

    fun load(name: String): JsonObject = Json.parseToJsonElement(File(dir, "$name.json").readText()).jsonObject

    fun sound(id: String): Sound = requireNotNull(Sound.fromId(id)) { "unknown sound $id" }

    fun side(id: String): Side = requireNotNull(Side.fromId(id)) { "unknown side $id" }

    fun line(json: JsonObject) = Line(
        count = json.int("count"),
        sound = sound(json.string("sound")),
        dots = json.array("dots").map { it.jsonPrimitive.boolean },
        muted = json.bool("muted"),
    )
}

// Strict accessors: a missing or misspelled field fails the test instead of
// quietly testing a default.
fun JsonObject.field(name: String): JsonElement = requireNotNull(get(name)) { "missing field \"$name\" in $this" }

fun JsonObject.obj(name: String): JsonObject = field(name).jsonObject

fun JsonObject.objOrNull(name: String): JsonObject? = field(name).let { if (it is JsonNull) null else it.jsonObject }

fun JsonObject.array(name: String): JsonArray = field(name).jsonArray

fun JsonObject.objects(name: String): List<JsonObject> = array(name).map { it.jsonObject }

fun JsonObject.string(name: String): String = field(name).jsonPrimitive.also { require(it.isString) { "\"$name\" is not a string" } }.content

fun JsonObject.stringOrNull(name: String): String? = field(name).let { if (it is JsonNull) null else it.jsonPrimitive.content }

fun JsonObject.double(name: String): Double = field(name).jsonPrimitive.double

fun JsonObject.int(name: String): Int = field(name).jsonPrimitive.int

fun JsonObject.bool(name: String): Boolean = field(name).jsonPrimitive.boolean
