package org.futurerobotics.jargon

/**
 * returns `this` as a java.lang.Object.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
inline val Any.javaObj: Object
    get() = this as Object
