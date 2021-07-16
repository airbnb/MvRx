package com.airbnb.mvrx

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KProperty1

// Set of [MavericksView identity hash codes that have a pending invalidate.
private val pendingInvalidates = HashSet<Int>()
private val handler = Handler(Looper.getMainLooper()) { message ->
    val view = message.obj as MavericksView
    pendingInvalidates.remove(System.identityHashCode(view))
    if (view.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.invalidate()
    true
}

/**
 * If any callbacks to run [MavericksView.invalidate] have been posted with [MavericksView.postInvalidate]
 * then this cancels them.
 *
 * This may be useful if you have manually run [MavericksView.invalidate] and want to avoid the overhead
 * of having it run again on the next frame.
 */
fun MavericksView.cancelPendingInvalidates() {
    val viewHashCode = System.identityHashCode(this)
    if (pendingInvalidates.remove(viewHashCode)) {
        // the hashcode is used as the "what" in the message
        handler.removeMessages(viewHashCode)
    }
}

interface MavericksView : LifecycleOwner {
    /**
     * Override this to supply a globally unique id for this MvRxView. If your [MavericksView] is being recreated due to
     * a lifecycle event (e.g. rotation) you should assign a consistent id. Likely this means you should save the id
     * in onSaveInstance state. The viewId will not be accessed until a subscribe method is called.
     * Accessing mvrxViewId before calling super.onCreate() will cause a crash.
     */
    val mvrxViewId: String
        get() = when (this) {
            is ViewModelStoreOwner -> ViewModelProvider(this).get(MavericksViewIdViewModel::class.java).mavericksViewId
            else -> error(
                "If your MavericksView is not a ViewModelStoreOwner, you must implement mvrxViewId " +
                    "and return a string that is unique to this view and persistent across its entire lifecycle."
            )
        }

    /**
     * Override this to handle any state changes from [MavericksViewModel]s created through MvRx Fragment delegates.
     */
    fun invalidate()

    /**
     * The [LifecycleOwner] to use when making new subscriptions. You may want to return different owners depending
     * on what state your [MavericksView] is in.
     *
     * Fragments are handled by the default implementation using their standard lifecycle owner for subscriptions made
     * in `onCreate` (cleared in `onDestroy`), or their _view_ lifecycle owner for subscriptions made in or
     * after `onCreateView` (cleared in `onDestroyView`). Using [Fragment.getViewLifecycleOwner] is necessary to
     * retrieve the view's lifecycle as early as `onCreateView`. This method throws an `IllegalStateException` when view
     * lifecycle is unavailable (eg. _before_ `onCreateView`) which is caught as a signal to fall back to the fragment's
     * standard lifecycle owner.
     *
     * For non-Fragments the default [subscriptionLifecycleOwner] is the same as the MvRxView's standard lifecycle owner.
     */
    val subscriptionLifecycleOwner: LifecycleOwner
        get() = try {
            (this as? Fragment)?.viewLifecycleOwner ?: this
        } catch (e: IllegalStateException) {
            this
        }

    fun postInvalidate() {
        if (pendingInvalidates.add(System.identityHashCode(this@MavericksView))) {
            handler.sendMessage(Message.obtain(handler, System.identityHashCode(this@MavericksView), this@MavericksView))
        }
    }

    /**
     * Return a [UniqueOnly] delivery mode with a unique id for this fragment. In rare circumstances, if you
     * make two identical subscriptions with the same (or all) properties in this fragment, provide a customId
     * to avoid collisions.
     *
     * @param customId An additional custom id to identify this subscription. Only necessary if there are two subscriptions
     * in this fragment with exact same properties (i.e. two subscribes, or two selectSubscribes with the same properties).
     */
    fun uniqueOnly(customId: String? = null): UniqueOnly {
        return UniqueOnly(listOfNotNull(mvrxViewId, UniqueOnly::class.simpleName, customId).joinToString("_"))
    }

    /**
     * Return a [Custom] delivery mode with a unique id for this fragment. In rare circumstances, if you
     * make two identical subscriptions with the same (or all) properties in this fragment, provide a customId
     * to avoid collisions.
     *
     * @param customId An additional custom id to identify this subscription. Only necessary if there are two subscriptions
     * in this fragment with exact same properties (i.e. two subscribes, or two selectSubscribes with the same properties).
     */
    fun custom(customId: String? = null, distinctOnly: Boolean, flowConfiguration: (Flow<*>, DeliveryMode) -> Flow<*>): Custom {
        return Custom(listOfNotNull(mvrxViewId, Custom::class.simpleName, customId).joinToString("_"), distinctOnly, flowConfiguration)
    }

    /**
     * Subscribes to all state updates for the given viewModel.
     *
     * @param deliveryMode If [UniqueOnly] when this [MavericksView] goes from a stopped to started lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    fun <S : MavericksState> MavericksViewModel<S>.onEach(deliveryMode: DeliveryMode = RedeliverOnStart, action: suspend (S) -> Unit) =
        _internal(subscriptionLifecycleOwner, deliveryMode, action)

    /**
     * Subscribes to state changes for only a specific property and calls the action with
     * only that single property.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    fun <S : MavericksState, A> MavericksViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A) -> Unit
    ) = _internal1(subscriptionLifecycleOwner, prop1, deliveryMode, action)

    /**
     * Subscribes to state changes for two properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart]
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted..
     */
    fun <S : MavericksState, A, B> MavericksViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B) -> Unit
    ) = _internal2(subscriptionLifecycleOwner, prop1, prop2, deliveryMode, action)

    /**
     * Subscribes to state changes for three properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    fun <S : MavericksState, A, B, C> MavericksViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C) -> Unit
    ) = _internal3(subscriptionLifecycleOwner, prop1, prop2, prop3, deliveryMode, action)

    /**
     * Subscribes to state changes for four properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    fun <S : MavericksState, A, B, C, D> MavericksViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C, D) -> Unit
    ) = _internal4(subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, deliveryMode, action)

    /**
     * Subscribes to state changes for five properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    fun <S : MavericksState, A, B, C, D, E> MavericksViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C, D, E) -> Unit
    ) = _internal5(subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, prop5, deliveryMode, action)

    /**
     * Subscribes to state changes for six properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    fun <S : MavericksState, A, B, C, D, E, F> MavericksViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        prop6: KProperty1<S, F>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C, D, E, F) -> Unit
    ) = _internal6(subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, prop5, prop6, deliveryMode, action)

    /**
     * Subscribes to state changes for seven properties.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    fun <S : MavericksState, A, B, C, D, E, F, G> MavericksViewModel<S>.onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        prop6: KProperty1<S, F>,
        prop7: KProperty1<S, G>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        action: suspend (A, B, C, D, E, F, G) -> Unit
    ) = _internal7(subscriptionLifecycleOwner, prop1, prop2, prop3, prop4, prop5, prop6, prop7, deliveryMode, action)

    /**
     * Subscribe to changes in an async property. There are optional parameters for onSuccess
     * and onFail which automatically unwrap the value or error.
     *
     * @param deliveryMode If [UniqueOnly], when this [MavericksView] goes from a stopped to start lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     * @param onFail supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     * @param onSuccess supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    fun <S : MavericksState, T> MavericksViewModel<S>.onAsync(
        asyncProp: KProperty1<S, Async<T>>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        onFail: (suspend (Throwable) -> Unit)? = null,
        onSuccess: (suspend (T) -> Unit)? = null
    ) = _internalSF(subscriptionLifecycleOwner, asyncProp, deliveryMode, onFail, onSuccess)
}
