package dev.dkong.copypaste.objects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Position(
    val x: Float,
    val y: Float
)

@Serializable
data class Action(
    @SerialName("act_type")
    val actType: String,
    @SerialName("first_frame")
    val firstFrame: Int,
    @SerialName("resulting_screen_ocr")
    val resultingScreenOcr: String,
    val taps: Array<Position>
) {
    override fun toString(): String {
        return "Type: $actType, First frame: $firstFrame, OCR: $resultingScreenOcr"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Action

        if (actType != other.actType) return false
        if (firstFrame != other.firstFrame) return false
        if (resultingScreenOcr != other.resultingScreenOcr) return false
        if (!taps.contentEquals(other.taps)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = actType.hashCode()
        result = 31 * result + firstFrame
        result = 31 * result + resultingScreenOcr.hashCode()
        result = 31 * result + taps.contentHashCode()
        return result
    }
}

@Serializable
data class Sequence(
    val result: Array<Action>? = null,
    val state: String? = null,
    val status: String? = null,
    var name: String? = null
) {
    override fun toString(): String {
        return "Name: $name | Actions: ${
            result?.fold("") { acc, action ->
                "$acc\n$action"
            }
        }"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Sequence

        if (!result.contentEquals(other.result)) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result1 = result.contentHashCode()
        result1 = 31 * result1 + state.hashCode()
        return result1
    }
}