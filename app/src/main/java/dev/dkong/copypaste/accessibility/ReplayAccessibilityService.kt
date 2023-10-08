package dev.dkong.copypaste.accessibility

import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.ImagePainter
import dev.dkong.copypaste.R
import dev.dkong.copypaste.objects.Action
import dev.dkong.copypaste.objects.Position
import dev.dkong.copypaste.objects.Sequence
import dev.dkong.copypaste.utils.ActionManager
import dev.dkong.copypaste.utils.EditDistance
import dev.dkong.copypaste.utils.EditDistance.min
import dev.dkong.copypaste.utils.ExecutionManager
import dev.dkong.copypaste.utils.ExecutionStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

class ReplayAccessibilityService : AccessibilityService() {
    private fun actionView(): ComposeView {
        val view = ComposeView(this)
        val actionTimer = (0..Int.MAX_VALUE)
            .asSequence()
            .asFlow()
            .onEach { delay(1000L) }
        view.setContent {
            /**
             * The current progress through the actions inside the sequence
             */
            var actionIndex by remember { mutableStateOf(0) }
            var actionStep by remember { mutableStateOf(ExecutionManager.step) }
            var actionIntervention by remember { mutableStateOf(ExecutionManager.intervention) }
            ExecutionManager.stepChangeListeners.add { newStep -> actionStep = newStep }
            ExecutionManager.interventionChangeListeners.add { newIntervention ->
                actionIntervention = newIntervention
            }
            var sequence by remember { mutableStateOf(ExecutionManager.currentSequence) }
            val sequenceActions = remember { mutableStateListOf<Action>() }
            if (sequenceActions.size == 0) {
                sequenceActions.addAll(
                    ExecutionManager.currentSequence?.result?.toList() ?: listOf()
                )
            }
            ExecutionManager.sequenceChangeListeners.add { newSequence ->
                sequence = newSequence
                newSequence?.result?.let { r ->
                    sequenceActions.clear()
                    sequenceActions.addAll(r)
                    actionIndex = 0
                }
            }
            var actionInProgress by remember { mutableStateOf(false) }
            if (actionStep < ExecutionStep.Complete) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 8.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 8.dp
                            )
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Waiting for app to be opened
                        if (actionStep == ExecutionStep.OpenApp) {
                            FloatingActionButton(
                                onClick = {
                                    ExecutionManager.step = ExecutionStep.InProgress
                                },
                                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                            ) {
                                Image(
                                    painterResource(id = R.drawable.outline_play_arrow_24),
                                    "Action"
                                )
                            }
                        }
                        if (actionStep == ExecutionStep.InProgress) {
                            // Run a flow to execute the actions
                            LaunchedEffect(Unit) {
                                actionTimer.collect {
                                    if (actionInProgress) return@collect
                                    // Replay the next action (for now)
                                    if (actionIndex >= sequenceActions.size) {
                                        // We have finished replaying the actions
                                        ExecutionManager.stop()
                                        return@collect
                                    }
                                    val executingAction = sequenceActions[actionIndex]
                                    if (ExecutionManager.intervention) {
                                        // User needs to intervene the action; wait
                                        return@collect
                                    } else {
                                        val previousAction =
                                            sequenceActions[maxOf(0, actionIndex - 1)]
                                        // If previous trigger was a tap, check whether the screen is correct
                                        if (previousAction.actType == Action.ActionType.Tap) {
                                            // Read the screen contents
                                            rootInActiveWindow?.let { n ->
                                                val screenContent =
                                                    nodeToText(n)
                                                val distanceLength = minOf(
                                                    screenContent.length,
                                                    executingAction.resultingScreenOcr.length
                                                )
                                                val editDistance =
                                                    EditDistance.editDistance(
                                                        screenContent.substring(0, distanceLength),
                                                        executingAction.resultingScreenOcr
                                                            .substring(
                                                                0,
                                                                distanceLength
                                                            )
                                                    ).toFloat()
                                                val mismatch = editDistance / distanceLength
                                                // The greater the edit distance, the less of a match
                                                val distanceThreshold = 0.5f
                                                Log.d(
                                                    "REPLAY",
                                                    "Screen is ${(1 - mismatch) * 100}% match, distance is $editDistance"
                                                )
//                                                Log.d(
//                                                    "REPLAY",
//                                                    "Expected string is ${executingAction.resultingScreenOcr}"
//                                                )
//                                                Log.d(
//                                                    "REPLAY",
//                                                    "Actual string is $screenContent"
//                                                )
                                                if (mismatch > distanceThreshold) {
                                                    // The screen is not a match; user needs to intervene
                                                    performGlobalAction(GLOBAL_ACTION_BACK)
                                                    ExecutionManager.intervention = true
                                                    return@collect
                                                }
                                            }
                                        }
                                    }
                                    // Execute the action
                                    actionInProgress = true

                                    sequence?.let { seq ->
                                        // Determine the duration of the gesture -- used to help with swipe acceleration timing
                                        // Get the duration in frames based on the data
                                        val frameDuration =
                                            if (actionIndex < sequenceActions.size - 1)
                                                (seq.result?.get(actionIndex + 1)?.firstFrame?.minus(
                                                    executingAction.firstFrame
                                                ) ?: 0L).toLong() else 0L
                                        // Get the duration in milliseconds based on 30 frames per second
                                        val realTimeDuration = frameDuration / 30L
                                        if (executingAction.actType == Action.ActionType.Swipe)
                                            Log.d("REPLAY", "Duration is $realTimeDuration")

                                        executingAction.toExecutableAction(
                                            service = this@ReplayAccessibilityService,
                                            sequence = seq,
                                            duration = if (realTimeDuration > 0) realTimeDuration else 250L
                                        )
                                            ?.execute(
                                                object : GestureResultCallback() {
                                                    override fun onCompleted(gestureDescription: GestureDescription?) {
                                                        // Gesture finished; continue
                                                        actionInProgress = false
                                                    }

                                                    override fun onCancelled(gestureDescription: GestureDescription?) {
                                                        onCompleted(gestureDescription)
                                                    }
                                                }
                                            )
                                        actionIndex += 1
                                    }
                                }
                            }
                            if (actionIntervention) {
                                // Show the intervention completion button
                                FloatingActionButton(
                                    onClick = {
                                        ExecutionManager.intervention = false
                                    },
                                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                                ) {
                                    Icon(Icons.Default.Check, "Done")
                                }
                            }
                            OutlinedButton(onClick = {
                                ExecutionManager.stop()
                            }, enabled = !actionInProgress) {
                                Text("Stop", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        return view
    }

    private fun infoView(): ComposeView {
        val view = ComposeView(this)
        view.setContent {
            var actionStep by remember { mutableStateOf(ExecutionManager.step) }
            var actionInProgress by remember { mutableStateOf(false) }
            var actionIntervention by remember { mutableStateOf(ExecutionManager.intervention) }
            ExecutionManager.interventionChangeListeners.add { newIntervention ->
                actionIntervention = newIntervention
            }
            ExecutionManager.stepChangeListeners.add { newStep -> actionStep = newStep }
            var prompt by remember { mutableStateOf("") }
            prompt = when (actionStep) {
                ExecutionStep.None -> "Idle"
                ExecutionStep.OpenApp -> "Open the app"
                ExecutionStep.InProgress -> "Replaying"
                else -> ""
            }
            if (actionIntervention) {
                prompt = "Intervention required"
            }
            if (prompt != "") {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 0.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            )
                        )
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        return view
    }

    override fun onServiceConnected() {
        serviceInfo.flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
        // Render the UI

        // Partly based on
        // https://stackoverflow.com/questions/75709422/a-floating-window-in-jetpack-compose

        // Set the window parameters for the overlay
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        fun addView(view: ComposeView, grav: Int) {
            val params = WindowManager.LayoutParams()
            params.apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                gravity = grav
                format = PixelFormat.TRANSPARENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }

            // Set up the lifecycle stuff
            val lifecycleOwner = ComposeLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            view.setViewTreeLifecycleOwner(lifecycleOwner)
            view.setViewTreeViewModelStoreOwner(view.findViewTreeViewModelStoreOwner())

            // Ensure the view re-composes upon updates
            val coroutineContext = AndroidUiDispatcher.CurrentThread
            val runRecomposeScope = CoroutineScope(coroutineContext)
            val recomposer = Recomposer(coroutineContext)
            view.compositionContext = recomposer
            runRecomposeScope.launch {
                recomposer.runRecomposeAndApplyChanges()
            }

            // Add the view to the window
            wm.addView(view, params)
        }

        // Create the Compose view
        addView(actionView(), grav = Gravity.START or Gravity.CENTER_VERTICAL)
        addView(infoView(), grav = Gravity.TOP or Gravity.CENTER_HORIZONTAL)
    }

    /**
     * Swipe down on the screen
     */
    fun swipe(
        from: Position,
        to: Position,
        callback: GestureResultCallback? = null,
        duration: Long = 250
    ) {
        val path = Path()
        path.moveTo(from.x, from.y)
        path.lineTo(to.x, to.y)
        val gesture = GestureDescription.Builder()
        gesture.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        dispatchGesture(gesture.build(), callback, null)
    }

    /**
     * Tap on the screen
     */
    fun tap(
        point: Position,
        callback: GestureResultCallback? = null
    ) {
        val path = Path()
        path.moveTo(point.x, point.y)
        path.lineTo(point.x + 1, point.y + 1)
        val gesture = GestureDescription.Builder()
        gesture.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
        dispatchGesture(gesture.build(), callback, null)
    }

    /**
     * Long press on the screen
     */
    fun longTap(
        point: Position,
        callback: GestureResultCallback? = null,
        duration: Long = 1000
    ) {
        val path = Path()
        path.moveTo(point.x, point.y)
        path.lineTo(point.x + 1, point.y + 1)
        val gesture = GestureDescription.Builder()
        gesture.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        dispatchGesture(gesture.build(), callback, null)
    }

    /**
     * Listen for specific accessibility events
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        when (event?.eventType) {
//            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
//                screenNodes = rootInActiveWindow
//            }
//        }
    }

//    override fun onGesture(gestureEvent: AccessibilityGestureEvent): Boolean {
//        // Check if Android S or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            Log.d("Gesture", gestureEvent.motionEvents.toString())
//        }
//
//        return super.onGesture(gestureEvent)
//    }

    /**
     * Convert a node's content to a string, recursively
     */
    private fun nodeToText(node: AccessibilityNodeInfo): String {
        var childrenText = ""
        for (i in 0 until node.childCount) {
            try {
                childrenText += nodeToText(node.getChild(i))
            } catch (e: NullPointerException) {
                continue
            }
        }
        val nodeText = if (node.text == null) "" else node.text.toString()
        return nodeText + childrenText
    }

    override fun onInterrupt() {
        return
    }
}