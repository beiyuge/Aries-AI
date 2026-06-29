package com.ai.phoneagent.core.capability

class CapabilityResult<out T> private constructor(
    private val value: T?,
    private val error: CapabilityError?,
) {
    val isSuccess: Boolean = error == null

    fun getOrNull(): T? = value

    fun errorOrNull(): CapabilityError? = error

    companion object {
        fun <T> success(value: T): CapabilityResult<T> = CapabilityResult(value, null)

        fun <T> failure(error: CapabilityError): CapabilityResult<T> = CapabilityResult(null, error)
    }
}
