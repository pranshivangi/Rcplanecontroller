package com.gamepadbt

import android.view.InputDevice
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.roundToInt

class GamepadHandler {
    // Joystick values (-100 to 100)
    var leftX: Int = 0
        private set
    
    var rightX: Int = 0
        private set
    
    var rightY: Int = 0
        private set

    // Deadzone for joysticks (values less than this are considered 0)
    private val deadzone = 0.1f

    fun processJoystickInput(event: MotionEvent) {
        // Get the InputDevice that generated this event
        val device = event.device
        
        // Calculate the raw joystick values (range -1 to 1)
        val rawLeftX = getCenteredAxis(event, device, MotionEvent.AXIS_X) ?: 0f
        val rawRightX = getCenteredAxis(event, device, MotionEvent.AXIS_Z) ?: 0f
        val rawRightY = getCenteredAxis(event, device, MotionEvent.AXIS_RZ) ?: 0f
        
        // Apply deadzone and scale to -100 to 100 range
        leftX = scaleJoystickValue(rawLeftX)
        rightX = scaleJoystickValue(rawRightX)
        rightY = scaleJoystickValue(rawRightY)
    }

    private fun getCenteredAxis(
        event: MotionEvent,
        device: InputDevice,
        axis: Int
    ): Float? {
        // Get the axis range
        val range = device.getMotionRange(axis, event.source) ?: return null
        
        // Calculate the center value (neutral position)
        val flat = range.flat
        val value = event.getAxisValue(axis)
        
        // Return a centered value between -1 and 1
        return if (abs(value) > flat) {
            value
        } else {
            0f
        }
    }

    private fun scaleJoystickValue(value: Float): Int {
        // Apply deadzone
        val processedValue = if (abs(value) < deadzone) 0f else value
        
        // Scale to -100 to 100 and convert to integer
        return (processedValue * 100).roundToInt()
    }
}
