package dev.dkong.copypaste.objects

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.content.res.Resources
import dev.dkong.copypaste.accessibility.ReplayAccessibilityService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class ExecutableAction(val service: ReplayAccessibilityService, val action: Action) {
    abstract fun execute(
        callback: GestureResultCallback? = null
    )

    class SwipeAction(service: ReplayAccessibilityService, action: Action, val dimensions: Position) :
        ExecutableAction(service, action) {
        override fun execute(
            callback: GestureResultCallback?
        ) {
            // Calculate the scaling of the original action with the device's resolution
            val xScale = if (action.dimensions != null) dimensions.x / dimensions.x else 1f
            val yScale = if (action.dimensions != null) dimensions.y / dimensions.y else 1f

            // Scale the tap coordinates as needed
            service.swipe(
                Position(action.taps.first().x * xScale, action.taps.first().y * yScale),
                Position(action.taps.last().x * xScale, action.taps.last().y * yScale),
                callback
            )
        }
    }

    class TapAction(service: ReplayAccessibilityService, action: Action) :
        ExecutableAction(service, action) {
        override fun execute(
            callback: GestureResultCallback?
        ) {
            service.tap(
                action.taps.first(),
                callback
            )
        }
    }

    class LongTapAction(service: ReplayAccessibilityService, action: Action) :
        ExecutableAction(service, action) {
        override fun execute(
            callback: GestureResultCallback?
        ) {
            service.longTap(
                action.taps.first(),
                callback
            )
        }
    }
}

@Serializable
data class Position(
    val x: Float,
    val y: Float
)

@Serializable
data class Action(
    @SerialName("act_type")
    val actType: ActionType?,
    @SerialName("action_hint")
    val actionHint: String? = null,
    @SerialName("first_frame")
    val firstFrame: Int,
    @SerialName("resulting_screen_ocr")
    val resultingScreenOcr: String,
    var dimensions: Position? = null,
    val taps: Array<Position>
) {
    @Serializable
    enum class ActionType {
        @SerialName("SWIPE")
        Swipe,

        @SerialName("CLICK")
        Tap,

        @SerialName("LONG_CLICK")
        LongTap
    }

    /**
     * Get an executable action for this parsed action
     */
    fun toExecutableAction(service: ReplayAccessibilityService): ExecutableAction? {
        val dimensions = Position(
            Resources.getSystem().displayMetrics.widthPixels.toFloat(),
            Resources.getSystem().displayMetrics.heightPixels.toFloat()
        )
        return when (actType) {
            ActionType.Swipe -> ExecutableAction.SwipeAction(service, this, dimensions = dimensions)
            ActionType.Tap -> ExecutableAction.TapAction(service, this)
            ActionType.LongTap -> ExecutableAction.LongTapAction(service, this)
            else -> null
        }
    }

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
    var name: String? = null,
    var id: Long? = null,
    var creationTime: Long? = null,
    var dimensions: Position? = null
) {
    override fun toString(): String {
        return "Name: $name | Actions: ${
            result?.fold("") { acc, action ->
                "$acc\n$action"
            }
        }"
    }

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        other as Sequence
        return this.id == other.id
    }

    override fun hashCode(): Int {
        var result1 = result.contentHashCode()
        result1 = 31 * result1 + state.hashCode()
        return result1
    }
}
