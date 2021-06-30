package com.airbnb.mvrx

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KProperty1

/**
 * Defines what updates a subscription should receive.
 * See: [RedeliverOnStart], [UniqueOnly].
 */
sealed class DeliveryMode {
    internal fun appendPropertiesToId(vararg properties: KProperty1<*, *>): DeliveryMode {
        return when (this) {
            is RedeliverOnStart -> RedeliverOnStart
            is UniqueOnly -> UniqueOnly(properties.joinToString(",", prefix = subscriptionId + "_") { it.name })
            is Custom -> Custom(
                properties.joinToString(",", prefix = subscriptionId + "_") { it.name },
                this.distinctOnly,
                this.flowConfiguration
            )
        }
    }
}

/**
 * The subscription will receive the most recent state update when transitioning from locked to unlocked states (stopped -> started),
 * even if the state has not changed while locked.
 *
 * Likewise, when a [MavericksView] resubscribes after a configuration change the most recent update will always be emitted.
 */
object RedeliverOnStart : DeliveryMode()

/**
 * The subscription will receive the most recent state update when transitioning from locked to unlocked states (stopped -> started),
 * only if the state has changed while locked. This will include the initial state as a state update.
 *
 * Likewise, when a [MavericksView] resubscribes after a configuration change the most recent update will only be emitted
 * if the state has changed while locked.
 *
 * @param subscriptionId A uniqueIdentifier for this subscription. It is an error for two unique only subscriptions to
 * have the same id.
 */
class UniqueOnly(val subscriptionId: String) : DeliveryMode()

/**
 * The subscription will receive the most recent state update when transitioning from locked to unlocked states (stopped -> started) after the
 * caller's provided custom behavior via [flowConfiguration]. If [distinctOnly] is true, this will behave like []UniqueOnly].
 *
 * Likewise, when a [MavericksView] resubscribes after a configuration change the most recent update will be emitting after processing the desired
 * custom behavior.
 *
 * @param subscriptionId A uniqueIdentifier for this subscription. It is an error for two unique only subscriptions to
 * have the same id.
 */
class Custom(val subscriptionId: String, val distinctOnly: Boolean, val flowConfiguration: (Flow<*>, DeliveryMode) -> Flow<*>) : DeliveryMode()
