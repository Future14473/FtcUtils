package org.futurerobotics.jargon.ftcbridge

import com.qualcomm.robotcore.hardware.DcMotorEx
import org.futurerobotics.jargon.hardware.DcMotor
import org.futurerobotics.jargon.math.TAU
import com.qualcomm.robotcore.hardware.DcMotor as InnerMotor

/**
 * A wrapper around a ftc [DcMotorEx] for a jargon [DcMotor].
 *
 * @param [motor] the motor.
 * @param [ticksPerRev] the ticks per revolution.
 */
class FtcMotor(val motor: DcMotorEx, val ticksPerRev: Double) : DcMotor {

    /** Gets raw encoder position. */
    val encoderPosition: Int get() = motor.currentPosition
    override val maxVoltage: Double get() = 12.0
    override var voltage: Double
        get() = motor.power * maxVoltage
        set(value) {
            motor.power = (value / maxVoltage).coerceIn(-1.0, 1.0)
        }
    override val position: Double get() = motor.currentPosition / ticksPerRev * TAU
    override val velocity: Double
        get() = motor.velocity / ticksPerRev * TAU

    override fun resetPosition() {
        motor.mode = InnerMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = InnerMotor.RunMode.RUN_WITHOUT_ENCODER
    }

    override fun init() {
        motor.mode = InnerMotor.RunMode.RUN_WITHOUT_ENCODER
        motor.zeroPowerBehavior = InnerMotor.ZeroPowerBehavior.FLOAT
        motor.motorType.ticksPerRev = ticksPerRev
        motor.power = 0.0
    }

    override fun stop() {
        motor.power = 0.0
    }
}

/**
 * Wraps this [DcMotorEx] into a [FtcMotor], with the given [ticksPerRev].
 */
fun DcMotorEx.wrap(ticksPerRev: Double): FtcMotor = FtcMotor(this, ticksPerRev)

