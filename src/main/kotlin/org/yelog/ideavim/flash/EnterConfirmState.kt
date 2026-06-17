package org.yelog.ideavim.flash

internal class EnterConfirmState {
    private var generation = 0
    private var acceptsEnter = false
    private var armed = false

    fun start(supportsEnterConfirm: Boolean, enabled: Boolean, scheduleArm: (() -> Unit) -> Unit): Boolean {
        generation += 1
        armed = false
        acceptsEnter = enabled && supportsEnterConfirm
        if (!acceptsEnter) {
            return false
        }

        val expectedGeneration = generation
        scheduleArm {
            if (generation == expectedGeneration) {
                armed = true
            }
        }
        return true
    }

    fun shouldConsumeEnter(): Boolean = acceptsEnter

    fun shouldHandleEnter(): Boolean = acceptsEnter && armed

    fun stop() {
        generation += 1
        acceptsEnter = false
        armed = false
    }
}
