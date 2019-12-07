package org.futurerobotics.jargon.ftcbridge

import org.futurerobotics.jargon.hardware.Servo
import com.qualcomm.robotcore.hardware.Servo as InnerServo

private typealias DoubleRange = ClosedFloatingPointRange<Double>

private val DoubleRange.size get() = endInclusive - start

/**
 * A wrapper around a [Ftc robotcore Servo][InnerServo] that maps angles to servo positions.
 *
 * A specified [servoRange] must be given that corresponds to an [angleRange].
 */
class FtcServo(
    /** The servo this wraps around. */
    val servo: InnerServo,
    private val servoRange: DoubleRange,
    private val angleRange: DoubleRange
) : Servo {


    private fun mapToAngle(servoValue: Double): Double =
        (servoValue - servoRange.start) / servoRange.size *
                angleRange.size + angleRange.start

    private fun mapToServo(angleValue: Double): Double =
        (angleValue - angleRange.start) / angleRange.size *
                servoRange.size + servoRange.start

    override var position: Double
        get() = mapToAngle(servo.position)
        set(value) = kotlin.run { servo.position = mapToServo(value) }
}
