package dev.dkong.copypaste.utils

import android.util.Log
import dev.dkong.copypaste.objects.Sequence
import kotlin.properties.Delegates

class SequenceNotLoadedException :
    Exception("Cannot start execution without a sequence set.")

enum class ExecutionStep {
    None,
    SetUp,
    OpenApp,
    InProgress,
    Complete
}

/**
 * Manager for executing a sequence of actions
 */
object ExecutionManager {
    /**
     * Listeners subscribed to changes in the execution step
     */
    var stepChangeListeners = ArrayList<(newStep: ExecutionStep) -> Unit>()

    /**
     * The current progress of the sequence execution
     */
    var step by Delegates.observable(ExecutionStep.None) { _, _, newValue ->
        stepChangeListeners.forEach { listener ->
            listener(newValue)
        }
    }

    /**
     * Listeners subscribed to changes in the set sequence
     */
    var sequenceChangeListeners = ArrayList<(newSequence: Sequence?) -> Unit>()

    /**
     * The current sequence either ready to be or being executed
     */
    private var currentSequence: Sequence? by Delegates.observable(null) { _, _, newValue ->
        sequenceChangeListeners.forEach { listener ->
            listener(newValue)
        }
    }

    /**
     * Whether there is a sequence being executed
     */
    private var inProgress: Boolean = false

    var interventionChangeListeners = ArrayList<(newIntervention: Boolean) -> Unit>()

    /**
     * Whether a user needs to intervene the execution
     */
    var intervention: Boolean by Delegates.observable(false) { _, _, newValue ->
        interventionChangeListeners.forEach { listener ->
            listener(newValue)
        }
    }

    /**
     * Set up a new sequence to be executed
     */
    fun setUpSequence(newSequence: Sequence) {
        // Any pre-setting checks for the sequence
        currentSequence = newSequence
        step = ExecutionStep.SetUp
    }

    /**
     * Notify the beginning of execution
     */
    fun start() {
        if (currentSequence == null) throw SequenceNotLoadedException()
        inProgress = true
        step = ExecutionStep.OpenApp
        Log.i("EXEC MAN", "Execution started!")
    }

    /**
     * Notify the termination of execution
     */
    fun stop() {
        inProgress = false
        intervention = false
        step = ExecutionStep.None
    }
}