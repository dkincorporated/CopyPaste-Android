package dev.dkong.copypaste.utils

import dev.dkong.copypaste.objects.Sequence

class SequenceNotLoadedException :
    Exception(message = "Cannot start execution without a sequence set.")

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
     * The current progress of the sequence execution
     */
    var step: ExecutionStep = ExecutionStep.None

    /**
     * The current sequence either ready to be or being executed
     */
    private var currentSequence: Sequence? = null

    /**
     * Whether there is a sequence being executed
     */
    private var inProgress: Boolean = false

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
    }

    /**
     * Notify the termination of execution
     */
    fun stop() {
        inProgress = false
    }
}