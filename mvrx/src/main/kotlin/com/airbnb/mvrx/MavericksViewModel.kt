package com.airbnb.mvrx

import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.yield
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KProperty1

/**
 * All Mavericks ViewModels must extend this class. In Mavericks, ViewModels are generic on a single state class. The ViewModel owns all
 * state modifications via [setState] and other classes may observe the state.
 *
 * From a [MavericksView]/Fragment, using the view model provider delegates will automatically subscribe to state updates in a lifecycle-aware way
 * and call [MavericksView.invalidate] whenever it changes.
 *
 * Other classes can observe the state via [stateFlow].
 */
abstract class MavericksViewModel<S : MavericksState>(
    initialState: S
) {

    // Use the same factory for the life of the viewmodel, as it might change after this viewmodel is created (especially during tests)
    private val configFactory = Mavericks.viewModelConfigFactory

    @Suppress("LeakingThis")
    @InternalMavericksApi
    val config: MavericksViewModelConfig<S> = configFactory.provideConfig(
        this,
        initialState
    )

    val viewModelScope = config.coroutineScope

    private val stateStore = config.stateStore
    private val lastDeliveredStates = ConcurrentHashMap<String, Any>()
    private val activeSubscriptions = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val tag by lazy { javaClass.simpleName }
    private val mutableStateChecker = if (config.debugMode) MutableStateChecker(initialState) else null

    /**
     * Synchronous access to state is not exposed externally because there is no guarantee that
     * all setState reducers have run yet.
     */
    internal val state: S
        get() = stateStore.state

    /**
     * Return the current state as a Flow. For certain situations, this may be more convenient
     * than subscribe and selectSubscribe because it can easily be composed with other
     * coroutines operations and chained with operators.
     *
     * This WILL emit the current state followed by all subsequent state updates.
     *
     * This is not a StateFlow to prevent the ViewModel from having synchronous access to state. withState { state -> } should
     * be used as it is guaranteed to be run after all pending setState reducers have run.
     */
    val stateFlow: Flow<S>
        get() = stateStore.flow

    init {
        if (config.debugMode) {
            viewModelScope.launch(Dispatchers.Default) {
                validateState(initialState)
            }
        }
    }

    @CallSuper
    open fun onCleared() {
        viewModelScope.cancel()
    }

    /**
     * Validates a number of properties on the state class. This cannot be called from the main thread because it does
     * a fair amount of reflection.
     */
    private fun validateState(initialState: S) {
        state::class.assertImmutability()
        // Assert that state can be saved and restored.
        val bundle = state.persistState(validation = true)
        bundle.restorePersistedState(initialState, validation = true)
    }

    /**
     * Call this to mutate the current state.
     * A few important notes about the state reducer.
     * 1) It will not be called synchronously or on the same thread. This is for performance and accuracy reasons.
     * 2) Similar to the execute lambda above, the current state is the state receiver so the `count` in `count + 1` is actually the count
     *    property of the state at the time that the lambda is called.
     * 3) In development, MvRx will do checks to make sure that your setState is pure by calling in multiple times. As a result, DO NOT use
     *    mutable variables or properties from outside the lambda or else it may crash.
     */
    protected fun setState(reducer: S.() -> S) {
        if (config.debugMode) {
            // Must use `set` to ensure the validated state is the same as the actual state used in reducer
            // Do not use `get` since `getState` queue has lower priority and the validated state would be the state after reduced
            stateStore.set {
                val firstState = this.reducer()
                val secondState = this.reducer()

                if (firstState != secondState) {
                    @Suppress("UNCHECKED_CAST")
                    val changedProp = firstState::class.java.declaredFields.asSequence()
                        .onEach { it.isAccessible = true }
                        .firstOrNull { property ->
                            @Suppress("Detekt.TooGenericExceptionCaught")
                            try {
                                property.get(firstState) != property.get(secondState)
                            } catch (e: Throwable) {
                                false
                            }
                        }
                    if (changedProp != null) {
                        throw IllegalArgumentException(
                            "Impure reducer set on ${this@MavericksViewModel::class.java.simpleName}! " +
                                "${changedProp.name} changed from ${changedProp.get(firstState)} " +
                                "to ${changedProp.get(secondState)}. " +
                                "Ensure that your state properties properly implement hashCode."
                        )
                    } else {
                        throw IllegalArgumentException(
                            "Impure reducer set on ${this@MavericksViewModel::class.java.simpleName}! Differing states were provided by the same reducer." +
                                "Ensure that your state properties properly implement hashCode. First state: $firstState -> Second state: $secondState"
                        )
                    }
                }
                mutableStateChecker?.onStateChanged(firstState)

                firstState
            }
        } else {
            stateStore.set(reducer)
        }
    }

    /**
     * Calling this function suspends until all pending setState reducers are run and then returns the latest state.
     * As a result, it is safe to call setState { } and assume that the result from a subsequent awaitState() call will have that state.
     */
    suspend fun awaitState(): S {
        val deferredState = CompletableDeferred<S>()
        withState(deferredState::complete)
        return deferredState.await()
    }

    /**
     * Access the current ViewModel state. Takes a block of code that will be run after all current pending state
     * updates are processed.
     */
    protected fun withState(action: (state: S) -> Unit) {
        stateStore.get(action)
    }

    /**
     * Run a coroutine and wrap its progression with [Async] property reduced to the global state.
     *
     * @param dispatcher A custom coroutine dispatcher that the coroutine will run on. If null, uses the dispatcher in [viewModelScope],
     *                  which defaults to [Dispatchers.Main.immediate] and can be overridden globally with [Mavericks.initialize].
     * @param retainValue A state property that, when set, will be called to retrieve an optional existing data value that will be retained across
     *                    subsequent Loading and Fail states. This is useful if you want to display the previously successful data when
     *                    refreshing.
     * @param reducer A reducer that is applied to the current state and should return the new state. Because the state is the receiver
     *                and is likely a data class, an implementation may look like: `{ copy(response = it) }`.
     */
    protected open fun <T : Any?> Deferred<T>.execute(
        dispatcher: CoroutineDispatcher? = null,
        retainValue: KProperty1<S, Async<T>>? = null,
        reducer: S.(Async<T>) -> S
    ) = suspend { await() }.execute(dispatcher, retainValue, reducer)

    /**
     * Run a coroutine and wrap its progression with [Async] property reduced to the global state.
     *
     * @param dispatcher A custom coroutine dispatcher that the coroutine will run on. If null, uses the dispatcher in [viewModelScope],
     *                  which defaults to [Dispatchers.Main.immediate] and can be overridden globally with [Mavericks.initialize].
     * @param retainValue A state property that, when set, will be called to retrieve an optional existing data value that will be retained across
     *                    subsequent Loading and Fail states. This is useful if you want to display the previously successful data when
     *                    refreshing.
     * @param reducer A reducer that is applied to the current state and should return the new state. Because the state is the receiver
     *                and is likely a data class, an implementation may look like: `{ copy(response = it) }`.
     */
    protected open fun <T : Any?> (suspend () -> T).execute(
        dispatcher: CoroutineDispatcher? = null,
        retainValue: KProperty1<S, Async<T>>? = null,
        reducer: S.(Async<T>) -> S
    ): Job {
        val blockExecutions = config.onExecute(this@MavericksViewModel)
        if (blockExecutions != MavericksViewModelConfig.BlockExecutions.No) {
            if (blockExecutions == MavericksViewModelConfig.BlockExecutions.WithLoading) {
                setState { reducer(Loading()) }
            }
            // Simulate infinite loading
            return viewModelScope.launch { delay(Long.MAX_VALUE) }
        }

        setState { reducer(Loading(value = retainValue?.get(this)?.invoke())) }

        return viewModelScope.launch(dispatcher ?: EmptyCoroutineContext) {
            try {
                val result = invoke()
                setState { reducer(Success(result)) }
            } catch (e: CancellationException) {
                @Suppress("RethrowCaughtException")
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                setState { reducer(Fail(e, value = retainValue?.get(this)?.invoke())) }
            }
        }
    }

    /**
     * Collect a Flow and wrap its progression with [Async] property reduced to the global state.
     *
     * @param dispatcher A custom coroutine dispatcher that the coroutine will run on. If null, uses the dispatcher in [viewModelScope],
     *                  which defaults to [Dispatchers.Main.immediate] and can be overridden globally with [Mavericks.initialize].
     * @param retainValue A state property that, when set, will be called to retrieve an optional existing data value that will be retained across
     *                    subsequent Loading and Fail states. This is useful if you want to display the previously successful data when
     *                    refreshing.
     * @param reducer A reducer that is applied to the current state and should return the new state. Because the state is the receiver
     *                and is likely a data class, an implementation may look like: `{ copy(response = it) }`.
     */
    protected open fun <T> Flow<T>.execute(
        dispatcher: CoroutineDispatcher? = null,
        retainValue: KProperty1<S, Async<T>>? = null,
        reducer: S.(Async<T>) -> S
    ): Job {
        val blockExecutions = config.onExecute(this@MavericksViewModel)
        if (blockExecutions != MavericksViewModelConfig.BlockExecutions.No) {
            if (blockExecutions == MavericksViewModelConfig.BlockExecutions.WithLoading) {
                setState { reducer(Loading(value = retainValue?.get(this)?.invoke())) }
            }
            // Simulate infinite loading
            return viewModelScope.launch { delay(Long.MAX_VALUE) }
        }

        setState { reducer(Loading(value = retainValue?.get(this)?.invoke())) }

        return catch { error -> setState { reducer(Fail(error, value = retainValue?.get(this)?.invoke())) } }
            .onEach { value -> setState { reducer(Success(value)) } }
            .launchIn(viewModelScope + (dispatcher ?: EmptyCoroutineContext))
    }

    /**
     * Collect a Flow and update state each time it emits a value. This is functionally the same as wrapping onEach with a setState call.
     *
     * @param dispatcher A custom coroutine dispatcher that the coroutine will run on. If null, uses the dispatcher in [viewModelScope],
     *                  which defaults to [Dispatchers.Main.immediate] and can be overridden globally with [Mavericks.initialize].
     * @param reducer A reducer that is applied to the current state and should return the new state. Because the state is the receiver
     *                and is likely a data class, an implementation may look like: `{ copy(response = it) }`.
     */
    protected open fun <T> Flow<T>.setOnEach(
        dispatcher: CoroutineDispatcher? = null,
        reducer: S.(T) -> S
    ): Job {
        val blockExecutions = config.onExecute(this@MavericksViewModel)
        if (blockExecutions != MavericksViewModelConfig.BlockExecutions.No) {
            // Simulate infinite work
            return viewModelScope.launch { delay(Long.MAX_VALUE) }
        }

        return onEach {
            setState { reducer(it) }
        }.launchIn(viewModelScope + (dispatcher ?: EmptyCoroutineContext))
    }

    /**
     * Subscribe to all state changes.
     *
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    protected fun onEach(
        action: suspend (S) -> Unit
    ) = _internal(null, RedeliverOnStart, action)

    /**
     * Subscribe to state changes for a single property.
     *
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    protected fun <A> onEach(
        prop1: KProperty1<S, A>,
        action: suspend (A) -> Unit
    ) = _internal1(null, prop1, action = action)

    /**
     * Subscribe to state changes for two properties.
     *
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    protected fun <A, B> onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        action: suspend (A, B) -> Unit
    ) = _internal2(null, prop1, prop2, action = action)

    /**
     * Subscribe to state changes for three properties.
     *
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    protected fun <A, B, C> onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        action: suspend (A, B, C) -> Unit
    ) = _internal3(null, prop1, prop2, prop3, action = action)

    /**
     * Subscribe to state changes for four properties.
     *
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    protected fun <A, B, C, D> onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        action: suspend (A, B, C, D) -> Unit
    ) = _internal4(null, prop1, prop2, prop3, prop4, action = action)

    /**
     * Subscribe to state changes for five properties.
     *
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    protected fun <A, B, C, D, E> onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        action: suspend (A, B, C, D, E) -> Unit
    ) = _internal5(null, prop1, prop2, prop3, prop4, prop5, action = action)

    /**
     * Subscribe to state changes for six properties.
     *
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    protected fun <A, B, C, D, E, F> onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        prop6: KProperty1<S, F>,
        action: suspend (A, B, C, D, E, F) -> Unit
    ) = _internal6(null, prop1, prop2, prop3, prop4, prop5, prop6, action = action)

    /**
     * Subscribe to state changes for seven properties.
     *
     * @param action supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    protected fun <A, B, C, D, E, F, G> onEach(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        prop4: KProperty1<S, D>,
        prop5: KProperty1<S, E>,
        prop6: KProperty1<S, F>,
        prop7: KProperty1<S, G>,
        action: suspend (A, B, C, D, E, F, G) -> Unit
    ) = _internal7(null, prop1, prop2, prop3, prop4, prop5, prop6, prop7, action = action)

    /**
     * Subscribe to changes in an async property. There are optional parameters for onSuccess
     * and onFail which automatically unwrap the value or error.
     *
     * @param onFail supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     * @param onSuccess supports cooperative cancellation. The previous action will be cancelled if it as not completed before
     * the next one is emitted.
     */
    protected fun <T> onAsync(
        asyncProp: KProperty1<S, Async<T>>,
        onFail: (suspend (Throwable) -> Unit)? = null,
        onSuccess: (suspend (T) -> Unit)? = null
    ) = _internalSF(null, asyncProp, RedeliverOnStart, onFail, onSuccess)

    @Suppress("EXPERIMENTAL_API_USAGE")
    internal fun <T : Any> Flow<T>.resolveSubscription(
        lifecycleOwner: LifecycleOwner? = null,
        deliveryMode: DeliveryMode,
        action: suspend (T) -> Unit
    ): Job {
        val flow = when {
            lifecycleOwner == null || MavericksTestOverrides.FORCE_DISABLE_LIFECYCLE_AWARE_OBSERVER -> this
            deliveryMode is UniqueOnly -> configureAndFilter(lifecycleOwner, deliveryMode.subscriptionId, true, deliveryMode) { flow, _ ->
                flow.dropWhile { it == lastDeliveredValue(deliveryMode.subscriptionId) }
            }
            deliveryMode is Custom -> {
                @Suppress("UNCHECKED_CAST")
                configureAndFilter(
                    lifecycleOwner,
                    deliveryMode.subscriptionId,
                    deliveryMode.distinctOnly,
                    deliveryMode,
                    deliveryMode.flowConfiguration as? ((Flow<T>, DeliveryMode) -> Flow<T>)
                )
            }
            else -> flowWhenStarted(lifecycleOwner)
        }

        val scope = (lifecycleOwner?.lifecycleScope ?: viewModelScope) + configFactory.subscriptionCoroutineContextOverride
        return scope.launch(start = CoroutineStart.UNDISPATCHED) {
            // Use yield to ensure flow collect coroutine is dispatched rather than invoked immediately.
            // This is necessary when Dispatchers.Main.immediate is used in scope.
            // Coroutine is launched with start = CoroutineStart.UNDISPATCHED to perform dispatch only once.
            yield()
            flow.collectLatest(action)
        }
    }

    private fun <T : Any> Flow<T>.configureAndFilter(
        lifecycleOwner: LifecycleOwner,
        subscriptionId: String,
        distinctOnly: Boolean,
        deliveryMode: DeliveryMode,
        flowConfiguration: ((Flow<T>, DeliveryMode) -> Flow<T>)?
    ): Flow<T> {
        return this.assertOneActiveSubscription(lifecycleOwner, subscriptionId)
            .run { flowConfiguration?.invoke(this, deliveryMode) ?: this }
            .flowWhenStarted(lifecycleOwner)
            .run { if (distinctOnly) distinctUntilChanged() else this }
            .onEach { lastDeliveredStates[subscriptionId] = it }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun <T> Flow<T>.assertOneActiveSubscription(owner: LifecycleOwner, subscriptionId: String): Flow<T> {
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                if (activeSubscriptions.contains(subscriptionId)) error(duplicateSubscriptionMessage(subscriptionId))
                activeSubscriptions += subscriptionId
            }

            override fun onDestroy(owner: LifecycleOwner) {
                activeSubscriptions.remove(subscriptionId)
            }
        }

        owner.lifecycle.addObserver(observer)
        return onCompletion {
            activeSubscriptions.remove(subscriptionId)
            owner.lifecycle.removeObserver(observer)
        }
    }

    fun <T> lastDeliveredValue(subscriptionId: String?): T? {
        @Suppress("UNCHECKED_CAST")
        return subscriptionId?.let { lastDeliveredStates[it] } as T?
    }

    private fun duplicateSubscriptionMessage(subscriptionId: String) = """
        Subscribing with a duplicate subscription id: $subscriptionId.
        If you have multiple uniqueOnly subscriptions in a MvRx view that listen to the same properties
        you must use a custom subscription id. If you are using a custom MvRxView, make sure you are using the proper
        lifecycle owner. See BaseMvRxFragment for an example.
    """.trimIndent()

    private fun <S : MavericksState> assertSubscribeToDifferentViewModel(viewModel: MavericksViewModel<S>) {
        require(this != viewModel) {
            "This method is for subscribing to other view models. Please pass a different instance as the argument."
        }
    }

    override fun toString(): String = "${this::class.java.simpleName} $state"
}
