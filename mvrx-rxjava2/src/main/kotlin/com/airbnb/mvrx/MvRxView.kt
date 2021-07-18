package com.airbnb.mvrx

import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty1

/**
 * Implement this in your MvRx capable Fragment.
 *
 * When you get a ViewModel with fragmentViewModel, activityViewModel, or existingViewModel, it
 * will automatically subscribe to all state changes in the ViewModel and call [invalidate].
 */
interface MvRxView : MavericksView {

    /**
     * Subscribes to all state updates for the given viewModel.
     *
     * @param deliveryMode If [UniqueOnly] when this MvRxView goes from a stopped to started lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MavericksState> BaseMvRxViewModel<S>.subscribe(deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) =
        _internal(subscriptionLifecycleOwner, deliveryMode, { subscriber(it) }).toDisposable()

    /**
     * Subscribes to state changes for only a specific property and calls the subscribe with
     * only that single property.
     *
     * @param deliveryMode If [UniqueOnly], when this MvRxView goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MavericksState, A> BaseMvRxViewModel<out S>.selectSubscribe(
        prop1: KProperty1<S, A>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        subscriber: (A) -> Unit
    ) = _internal1(subscriptionLifecycleOwner, prop1, deliveryMode, { subscriber(it) }).toDisposable()

    /**
     * Subscribe to changes in an async property. There are optional parameters for onSuccess
     * and onFail which automatically unwrap the value or error.
     *
     * @param deliveryMode If [UniqueOnly], when this MvRxView goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MavericksState, T> BaseMvRxViewModel<out S>.asyncSubscribe(
        asyncProp: KProperty1<S, Async<T>>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        onFail: ((Throwable) -> Unit)? = null,
        onSuccess: ((T) -> Unit)? = null
    ) = _internalSF(subscriptionLifecycleOwner, asyncProp, deliveryMode, { onFail?.invoke(it) }, { onSuccess?.invoke(it) }).toDisposable()

    /**
     * Subscribes to state changes for two properties.
     *
     * @param deliveryMode If [UniqueOnly], when this MvRxView goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MavericksState, A, B> BaseMvRxViewModel<out S>.selectSubscribe(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        subscriber: (A, B) -> Unit
    ) = _internal2(subscriptionLifecycleOwner, prop1, prop2, deliveryMode, { a, b -> subscriber(a, b) }).toDisposable()

    /**
     * Subscribes to state changes for three properties.
     *
     * @param deliveryMode If [UniqueOnly], when this MvRxView goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MavericksState, A, B, C> BaseMvRxViewModel<out S>.selectSubscribe(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        subscriber: (A, B, C) -> Unit
    ) = _internal3(subscriptionLifecycleOwner, prop1, prop2, prop3, deliveryMode, { a, b, c -> subscriber(a, b, c) }).toDisposable()

    /**
     * Subscribes to state changes for four properties.
     *
     * @param deliveryMode If [UniqueOnly], when this MvRxView goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MavericksState, A, B, C, D> BaseMvRxViewModel<out S>.selectSubscribe(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        subscriber: (A, B, C, D) -> Unit
    ) = _internal4(subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, deliveryMode, { a, b, c, d -> subscriber(a, b, c, d) }).toDisposable()

    /**
     * Subscribes to state changes for five properties.
     *
     * @param deliveryMode If [UniqueOnly], when this MvRxView goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MavericksState, A, B, C, D, E> BaseMvRxViewModel<out S>.selectSubscribe(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        subscriber: (A, B, C, D, E) -> Unit
    ) = _internal5(
        subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, prop5, deliveryMode,
        { a, b, c, d, e ->
            subscriber(a, b, c, d, e)
        }
    ).toDisposable()

    /**
     * Subscribes to state changes for six properties.
     *
     * @param deliveryMode If [UniqueOnly], when this MvRxView goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MavericksState, A, B, C, D, E, F> BaseMvRxViewModel<out S>.selectSubscribe(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        prop6: KProperty1<S, F>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        subscriber: (A, B, C, D, E, F) -> Unit
    ) = _internal6(
        subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, prop5, prop6, deliveryMode,
        { a, b, c, d, e, f ->
            subscriber(a, b, c, d, e, f)
        }
    ).toDisposable()

    /**
     * Subscribes to state changes for seven properties.
     *
     * @param deliveryMode If [UniqueOnly], when this MvRxView goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MavericksState, A, B, C, D, E, F, G> BaseMvRxViewModel<out S>.selectSubscribe(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        prop6: KProperty1<S, F>,
        prop7: KProperty1<S, G>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        subscriber: (A, B, C, D, E, F, G) -> Unit
    ) = _internal7(
        subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, prop5, prop6, prop7, deliveryMode,
        { a, b, c, d, e, f, g ->
            subscriber(a, b, c, d, e, f, g)
        }
    ).toDisposable()
}

private class JobDisposable(job: Job) : AtomicReference<Job>(job), Disposable {
    init {
        job.invokeOnCompletion { set(null) }
    }

    override fun dispose() {
        getAndSet(null)?.cancel()
    }

    override fun isDisposed(): Boolean = get()?.isActive?.not() ?: true
}

internal fun Job.toDisposable(): Disposable = JobDisposable(this)
