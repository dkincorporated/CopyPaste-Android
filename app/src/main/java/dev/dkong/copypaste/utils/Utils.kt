package dev.dkong.copypaste.utils

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser

object Utils {
    fun convertToJsonObject(response: String): JsonObject {
        val parser = Parser.default()
        val stringBuilder = StringBuilder(response)
        return parser.parse(stringBuilder) as JsonObject
    }

    fun convertToJsonArray(response: String): JsonArray<*> {
        val parser = Parser.default()
        val stringBuilder = StringBuilder(response)
        return parser.parse(stringBuilder) as JsonArray<*>
    }
}