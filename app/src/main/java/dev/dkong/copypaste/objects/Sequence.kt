package dev.dkong.copypaste.objects

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.content.res.Resources
import android.util.Log
import dev.dkong.copypaste.accessibility.ReplayAccessibilityService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class ExecutableAction(val service: ReplayAccessibilityService, val action: Action, val sequence: Sequence, val dimensions: Position) {
    abstract fun execute(
        callback: GestureResultCallback? = null
    )

    fun scalePosition(position: Position): Position {
        sequence.dimensions?.let { d ->
            // Calculate the scaling of the original dimensions to the current dimensions
            val xScale = dimensions.x / d.x
            val yScale = dimensions.y / d.y

            Log.d("ExecutableAction", "Scaling position by $xScale, $yScale")
            Log.d("ExecutableAction", "From: original $position, scaled (${position.x * xScale}, ${position.y * yScale})")

            return Position(position.x * xScale, position.y * yScale)
        }
        // Sequence dimensions not available; use the original coordinates
        Log.d("ExecutableAction", "No scaling possible; using original position")
        return position
    }

    class SwipeAction(
        service: ReplayAccessibilityService,
        sequence: Sequence,
        action: Action,
        dimensions: Position,
        private val duration: Long = 250
    ) :
        ExecutableAction(service, action, sequence, dimensions) {
        override fun execute(
            callback: GestureResultCallback?
        ) {
            val scaledFrom = scalePosition(action.taps.first())
            val scaledTo = scalePosition(action.taps.last())

            service.swipe(
                scaledFrom,
                scaledTo,
                callback,
                duration
            )

//            sequence.dimensions?.let { d ->
//                // Calculate the scaling of the original action with the device's resolution
//                val xScale = dimensions.x / d.x
//                val yScale = dimensions.y / d.y
//
//                Log.d("SwipeAction", "Scaling swipe action by $xScale, $yScale")
//                Log.d("SwipeAction", "From: original ${action.taps.first()}, scaled ${action.taps.first().x * xScale}, ${action.taps.first().y * yScale}")
//                Log.d("SwipeAction", "To: original ${action.taps.last()}, scaled ${action.taps.last().x * xScale}, ${action.taps.last().y * yScale}")
//
//                // Scale the tap coordinates as needed
//                service.swipe(
//                    Position(action.taps.first().x * xScale, action.taps.first().y * yScale),
//                    Position(action.taps.last().x * xScale, action.taps.last().y * yScale),
//                    callback,
//                    duration
//                )
//                return
//            }
//            // Sequence dimensions not available; use the original coordinates
//            service.swipe(
//                action.taps.first(),
//                action.taps.last(),
//                callback
//            )
        }
    }

    class TapAction(service: ReplayAccessibilityService, action: Action, sequence: Sequence, dimensions: Position) :
        ExecutableAction(service, action, sequence, dimensions) {
        override fun execute(
            callback: GestureResultCallback?
        ) {
            val scaledPosition = scalePosition(action.taps.first())

            service.tap(
                Position(scaledPosition.x, action.taps.first().y),
                callback
            )
        }
    }

    class LongTapAction(service: ReplayAccessibilityService, action: Action, sequence: Sequence, dimensions: Position) :
        ExecutableAction(service, action, sequence, dimensions) {
        override fun execute(
            callback: GestureResultCallback?
        ) {
            val scaledPosition = scalePosition(action.taps.first())

            service.longTap(
                scaledPosition,
                callback
            )
        }
    }
}

@Serializable
data class Position(
    val x: Float,
    val y: Float
) {
    override fun toString(): String {
        return "($x, $y)"
    }
}

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
    fun toExecutableAction(
        service: ReplayAccessibilityService,
        sequence: Sequence,
        duration: Long = 250 // for swipe actions only
    ): ExecutableAction? {
        val dimensions = Position(
            Resources.getSystem().displayMetrics.widthPixels.toFloat(),
            Resources.getSystem().displayMetrics.heightPixels.toFloat()
        )
        return when (actType) {
            ActionType.Swipe -> ExecutableAction.SwipeAction(
                service,
                sequence,
                this,
                dimensions = dimensions,
                duration = duration
            )

            ActionType.Tap -> ExecutableAction.TapAction(service, this, sequence, dimensions)
            ActionType.LongTap -> ExecutableAction.LongTapAction(service, this, sequence, dimensions)
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
