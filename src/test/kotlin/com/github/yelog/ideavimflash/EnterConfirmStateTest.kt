package com.github.yelog.vimflash

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.yelog.ideavim.flash.EnterConfirmState

class EnterConfirmStateTest : BasePlatformTestCase() {
    fun testSearchEnterIsNotHandledUntilDeferredArmRuns() {
        val state = EnterConfirmState()
        var deferredArm: (() -> Unit)? = null

        state.start(supportsEnterConfirm = true, enabled = true) { deferredArm = it }

        assertFalse(state.shouldHandleEnter())

        deferredArm?.invoke()

        assertTrue(state.shouldHandleEnter())
    }

    fun testDisabledEnterConfirmNeverHandlesEnter() {
        val state = EnterConfirmState()
        var deferredArm: (() -> Unit)? = null

        state.start(supportsEnterConfirm = true, enabled = false) { deferredArm = it }

        assertFalse(state.shouldHandleEnter())

        deferredArm?.invoke()

        assertFalse(state.shouldHandleEnter())
    }

    fun testUnsupportedModesNeverHandleEnterConfirm() {
        val state = EnterConfirmState()
        var deferredArm: (() -> Unit)? = null

        state.start(supportsEnterConfirm = false, enabled = true) { deferredArm = it }
        deferredArm?.invoke()

        assertFalse(state.shouldHandleEnter())
    }

    fun testStaleDeferredArmAfterStopDoesNotReEnableEnterConfirm() {
        val state = EnterConfirmState()
        var deferredArm: (() -> Unit)? = null

        state.start(supportsEnterConfirm = true, enabled = true) { deferredArm = it }
        state.stop()
        deferredArm?.invoke()

        assertFalse(state.shouldHandleEnter())
    }
}
