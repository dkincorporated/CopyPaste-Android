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
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.dkong.copypaste.objects.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ReplayAccessibilityService : AccessibilityService() {
    private fun actionView(): ComposeView {
        val view = ComposeView(this)
        view.setContent {
            var actionInProgress by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 16.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (actionInProgress) return@FloatingActionButton
                            actionInProgress = true
                            swipe(
                                from = Position(540f, 800f),
                                to = Position(540f, 1600f),
                                callback = object : GestureResultCallback() {
                                    override fun onCompleted(gestureDescription: GestureDescription?) {
                                        actionInProgress = false
                                    }

                                    override fun onCancelled(gestureDescription: GestureDescription?) {
                                        actionInProgress = false
                                    }
                                })
                        },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(
                            if (actionInProgress) Icons.Default.Warning else Icons.Default.KeyboardArrowUp,
                            "Action"
                        )
                    }
                    FloatingActionButton(
                        onClick = {
                            if (actionInProgress) return@FloatingActionButton
                            actionInProgress = true
                            swipe(
                                from = Position(540f, 1600f),
                                to = Position(540f, 800f),
                                callback = object : GestureResultCallback() {
                                    override fun onCompleted(gestureDescription: GestureDescription?) {
                                        actionInProgress = false
                                    }

                                    override fun onCancelled(gestureDescription: GestureDescription?) {
                                        onCompleted(gestureDescription)
                                    }
                                }
                            )
                        },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(
                            if (actionInProgress) Icons.Default.Warning else Icons.Default.KeyboardArrowDown,
                            "Action"
                        )
                    }
                    FloatingActionButton(
                        onClick = {
                            if (actionInProgress) return@FloatingActionButton
                            actionInProgress = true
                            longTap(
                                point = Position(540f, 1000f),
                                callback = object : GestureResultCallback() {
                                    override fun onCompleted(gestureDescription: GestureDescription?) {
                                        actionInProgress = false
                                    }

                                    override fun onCancelled(gestureDescription: GestureDescription?) {
                                        onCompleted(gestureDescription)
                                    }
                                }
                            )
                        },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(
                            if (actionInProgress) Icons.Default.Warning else Icons.Default.Add,
                            "Action"
                        )
                    }
//                    OutlinedButton(onClick = {}, enabled = !actionInProgress) {
//                        Text("Stop", color = MaterialTheme.colorScheme.error)
//                    }
                    Text(if (actionInProgress) "Playing" else "Idle")
                }
            }
        }
        return view
    }

    private fun infoView(): ComposeView {
        val view = ComposeView(this)
        view.setContent {
            var showPrompt by remember { mutableStateOf(true) }
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
                    if (showPrompt) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Open the app",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
//                            Text(
//                                text = "I was not able to find it myself.",
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
                        }
                    }
                }
            }
        }
        return view
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
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
        callback: GestureResultCallback? = null
    ) {
        val path = Path()
        path.moveTo(point.x, point.y)
        path.lineTo(point.x + 1, point.y + 1)
        val gesture = GestureDescription.Builder()
        gesture.addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
        dispatchGesture(gesture.build(), callback, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
        return
    }
}