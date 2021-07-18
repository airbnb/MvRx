@file:Suppress("FunctionName")

package com.airbnb.mvrx

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.reflect.KProperty1

@InternalMavericksApi
fun <VM : MavericksViewModel<out S>, S : MavericksState> VM._internal(
    owner: LifecycleOwner?,
    deliveryMode: DeliveryMode = RedeliverOnStart,
    action: suspend (S) -> Unit
) = stateFlow.resolveSubscription(owner, deliveryMode, action)

@InternalMavericksApi
fun <VM : MavericksViewModel<out S>, S : MavericksState, A> VM._internal1(
    owner: LifecycleOwner?,
    prop1: KProperty1<S, A>,
    deliveryMode: DeliveryMode = RedeliverOnStart,
    action: suspend (A) -> Unit
) = stateFlow
    .map { MavericksTuple1(prop1.get(it)) }
    .distinctUntilChanged()
    .resolveSubscription(owner, deliveryMode.appendPropertiesToId(prop1)) { (a) ->
        action(a)
    }

@InternalMavericksApi
fun <VM : MavericksViewModel<out S>, S : MavericksState, A, B> VM._internal2(
    owner: LifecycleOwner?,
    prop1: KProperty1<S, A>,
    prop2: KProperty1<S, B>,
    deliveryMode: DeliveryMode = RedeliverOnStart,
    action: suspend (A, B) -> Unit
) = stateFlow
    .map { MavericksTuple2(prop1.get(it), prop2.get(it)) }
    .distinctUntilChanged()
    .resolveSubscription(owner, deliveryMode.appendPropertiesToId(prop1, prop2)) { (a, b) ->
        action(a, b)
    }

@InternalMavericksApi
fun <VM : MavericksViewModel<out S>, S : MavericksState, A, B, C> VM._internal3(
    owner: LifecycleOwner?,
    prop1: KProperty1<S, A>,
    prop2: KProperty1<S, B>,
    prop3: KProperty1<S, C>,
    deliveryMode: DeliveryMode = RedeliverOnStart,
    action: suspend (A, B, C) -> Unit
) = stateFlow
    .map { MavericksTuple3(prop1.get(it), prop2.get(it), prop3.get(it)) }
    .distinctUntilChanged()
    .resolveSubscription(owner, deliveryMode.appendPropertiesToId(prop1, prop2, prop3)) { (a, b, c) ->
        action(a, b, c)
    }

@InternalMavericksApi
fun <VM : MavericksViewModel<out S>, S : MavericksState, A, B, C, D> VM._internal4(
    owner: LifecycleOwner?,
    prop1: KProperty1<S, A>,
    prop2: KProperty1<S, B>,
    prop3: KProperty1<S, C>,
    prop4: KProperty1<S, D>,
    deliveryMode: DeliveryMode = RedeliverOnStart,
    action: suspend (A, B, C, D) -> Unit
) = stateFlow
    .map { MavericksTuple4(prop1.get(it), prop2.get(it), prop3.get(it), prop4.get(it)) }
    .distinctUntilChanged()
    .resolveSubscription(owner, deliveryMode.appendPropertiesToId(prop1, prop2, prop3, prop4)) { (a, b, c, d) ->
        action(a, b, c, d)
    }

@InternalMavericksApi
fun <VM : MavericksViewModel<out S>, S : MavericksState, A, B, C, D, E> VM._internal5(
    owner: LifecycleOwner?,
    prop1: KProperty1<S, A>,
    prop2: KProperty1<S, B>,
    prop3: KProperty1<S, C>,
    prop4: KProperty1<S, D>,
    prop5: KProperty1<S, E>,
    deliveryMode: DeliveryMode = RedeliverOnStart,
    action: suspend (A, B, C, D, E) -> Unit
) = stateFlow
    .map { MavericksTuple5(prop1.get(it), prop2.get(it), prop3.get(it), prop4.get(it), prop5.get(it)) }
    .distinctUntilChanged()
    .resolveSubscription(owner, deliveryMode.appendPropertiesToId(prop1, prop2, prop3, prop4, prop5)) { (a, b, c, d, e) ->
        action(a, b, c, d, e)
    }

@InternalMavericksApi
fun <VM : MavericksViewModel<out S>, S : MavericksState, A, B, C, D, E, F> VM._internal6(
    owner: LifecycleOwner?,
    prop1: KProperty1<S, A>,
    prop2: KProperty1<S, B>,
    prop3: KProperty1<S, C>,
    prop4: KProperty1<S, D>,
    prop5: KProperty1<S, E>,
    prop6: KProperty1<S, F>,
    deliveryMode: DeliveryMode = RedeliverOnStart,
    action: suspend (A, B, C, D, E, F) -> Unit
) = stateFlow
    .map { MavericksTuple6(prop1.get(it), prop2.get(it), prop3.get(it), prop4.get(it), prop5.get(it), prop6.get(it)) }
    .distinctUntilChanged()
    .resolveSubscription(owner, deliveryMode.appendPropertiesToId(prop1, prop2, prop3, prop4, prop5, prop6)) { (a, b, c, d, e, f) ->
        action(a, b, c, d, e, f)
    }

@InternalMavericksApi
fun <VM : MavericksViewModel<out S>, S : MavericksState, A, B, C, D, E, F, G> VM._internal7(
    owner: LifecycleOwner?,
    prop1: KProperty1<S, A>,
    prop2: KProperty1<S, B>,
    prop3: KProperty1<S, C>,
    prop4: KProperty1<S, D>,
    prop5: KProperty1<S, E>,
    prop6: KProperty1<S, F>,
    prop7: KProperty1<S, G>,
    deliveryMode: DeliveryMode = RedeliverOnStart,
    action: suspend (A, B, C, D, E, F, G) -> Unit
) = stateFlow
    .map { MavericksTuple7(prop1.get(it), prop2.get(it), prop3.get(it), prop4.get(it), prop5.get(it), prop6.get(it), prop7.get(it)) }
    .distinctUntilChanged()
    .resolveSubscription(owner, deliveryMode.appendPropertiesToId(prop1, prop2, prop3, prop4, prop5, prop6, prop7)) { (a, b, c, d, e, f, g) ->
        action(a, b, c, d, e, f, g)
    }

@InternalMavericksApi
fun <VM : MavericksViewModel<out S>, S : MavericksState, T> VM._internalSF(
    owner: LifecycleOwner?,
    asyncProp: KProperty1<S, Async<T>>,
    deliveryMode: DeliveryMode = RedeliverOnStart,
    onFail: (suspend (Throwable) -> Unit)? = null,
    onSuccess: (suspend (T) -> Unit)? = null
) = _internal1(owner, asyncProp, deliveryMode.appendPropertiesToId(asyncProp)) { asyncValue ->
    if (onSuccess != null && asyncValue is Success) {
        onSuccess(asyncValue())
    } else if (onFail != null && asyncValue is Fail) {
        onFail(asyncValue.error)
    }
}
