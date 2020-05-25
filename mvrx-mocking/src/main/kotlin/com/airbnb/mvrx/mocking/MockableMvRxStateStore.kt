package com.airbnb.mvrx.mocking

import com.airbnb.mvrx.CoroutinesStateStore
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.ScriptableMvRxStateStore
import com.airbnb.mvrx.ScriptableStateStore
import com.airbnb.mvrx.mocking.MockBehavior.StateStoreBehavior
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow

interface MockableStateStore<S : Any> : ScriptableStateStore<S> {
    var mockBehavior: MockBehavior
}

/**
 * Allows switching between a mocked state store and a real state store behavior.
 * This is intended to enable testing.
 *
 * When [StateStoreBehavior.Scriptable] behavior is set, [next] can be called to force a state
 * synchronously. When in this mode, calls to [set] have no effect. In other modes,
 * it is an error to call [next].
 *
 * The mode can be dynamically changed to modify behavior at runtime.
 */
class MockableMvRxStateStore<S : MvRxState>(
    initialState: S,
    override var mockBehavior: MockBehavior
) : MockableStateStore<S> {
    private val scriptableStore = ScriptableMvRxStateStore(initialState)
    private val realStore = CoroutinesStateStore(initialState, GlobalScope) // TODO: Scope
    private val realImmediateStore = SynchronousMvRxStateStore(initialState)

    private val onStateSetListeners = mutableListOf<(previousState: S, newState: S) -> Unit>()
    private val onDisposeListeners = mutableListOf<(MockableMvRxStateStore<*>) -> Unit>()

    private val currentStore
        get() = when (mockBehavior.stateStoreBehavior) {
            StateStoreBehavior.Scriptable -> scriptableStore
            StateStoreBehavior.Normal -> realStore
            StateStoreBehavior.Synchronous -> realImmediateStore
        }

//    // Using "trampoline" scheduler so that updates are processed synchronously.
//    // This allows changes to be dispatched in a more controllable way for testing,
//    // to reduce flakiness in accounting for changes in screenshot tests.
//    override val observable: Observable<S>
//        get() = currentStore.observable.observeOn(Schedulers.trampoline())

    override val state: S
        get() = currentStore.state

//    override fun dispose() {
//        realStore.dispose()
//        realImmediateStore.dispose()
//        scriptableStore.dispose()
//        onDisposeListeners.forEach { it(this) }
//        onDisposeListeners.clear()
//    }

    override fun next(state: S) {
        check(mockBehavior.stateStoreBehavior == StateStoreBehavior.Scriptable) {
            "Scriptable store is not enabled"
        }

        scriptableStore.next(state)
        // Update the real stores too, so if we switch to them they are already up to date.
        realStore.set { state }
        realImmediateStore.set { state }
    }

    override fun get(block: (S) -> Unit) {
        currentStore.get(block)
    }
//
//    override fun isDisposed(): Boolean {
//        return realStore.isDisposed
//    }

    override fun set(stateReducer: S.() -> S) {
        val newState = state.stateReducer()
        if (onStateSetListeners.isNotEmpty()) {
            onStateSetListeners.forEach { it.invoke(state, newState) }
        }

        // Setting state only takes effect is "scriptable" mode is not enabled.
        if (mockBehavior.stateStoreBehavior != StateStoreBehavior.Scriptable) {
            // States are updated on all stores, so if we switch to any other in the future
            // it will be correctly up to date.
            realImmediateStore.set(stateReducer)
            realStore.set(stateReducer)
            scriptableStore.next(newState)
        }
    }

    /**
     * Add a listener that will be called each time a state is set on this state store via
     * the [set] function.
     */
    fun addOnStateSetListener(callback: (previousState: S, newState: S) -> Unit) {
        onStateSetListeners.add(callback)
    }

    fun removeOnStateSetListener(callback: (previousState: S, newState: S) -> Unit) {
        onStateSetListeners.remove(callback)
    }

    fun addOnDisposeListener(callback: (MockableMvRxStateStore<*>) -> Unit) {
        // TODO
//        if (isDisposed) {
//            callback(this)
//        } else {
//            onDisposeListeners.add(callback)
//        }
    }

    override val flow: Flow<S>
        get() = TODO("Not yet implemented")
}
