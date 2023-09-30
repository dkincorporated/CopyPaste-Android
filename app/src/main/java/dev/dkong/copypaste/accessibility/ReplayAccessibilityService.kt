package dev.dkong.copypaste.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
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
import dev.dkong.copypaste.utils.ExecutionManager
import dev.dkong.copypaste.utils.ExecutionStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ReplayAccessibilityService : AccessibilityService() {
    private fun actionView(): ComposeView {
        val view = ComposeView(this)
        val actionTimer = (0..Int.MAX_VALUE)
            .asSequence()
            .asFlow()
            .onEach { delay(1000L) }
        view.setContent {
            val scope = rememberCoroutineScope()
            /**
             * The current progress through the actions inside the sequence
             */
            var actionIndex by remember { mutableStateOf(0) }
            var actionStep by remember { mutableStateOf(ExecutionManager.step) }
            ExecutionManager.stepChangeListeners.add { newStep -> actionStep = newStep }
            var sequenceActions = remember { mutableStateListOf<Action>() }
            ExecutionManager.sequenceChangeListeners.add { newSequence ->
                newSequence?.result?.let { r ->
                    sequenceActions.clear()
                    sequenceActions.addAll(r)
                    actionIndex = 0
                }
            }
            var actionInProgress by remember { mutableStateOf(false) }
//            if (actionStep in ExecutionStep.SetUp..ExecutionStep.InProgress) {
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
                                    // Execute the action
                                    val executingAction = sequenceActions[actionIndex]
                                    actionInProgress = true
                                    executingAction.toExecutableAction(this@ReplayAccessibilityService)
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
            ExecutionManager.stepChangeListeners.add { newStep -> actionStep = newStep }
            var prompt by remember { mutableStateOf("") }
            prompt = when (actionStep) {
                ExecutionStep.None -> "Idle"
                ExecutionStep.OpenApp -> "Open the app"
                ExecutionStep.InProgress -> "Replaying"
                else -> ""
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
        return
    }
}