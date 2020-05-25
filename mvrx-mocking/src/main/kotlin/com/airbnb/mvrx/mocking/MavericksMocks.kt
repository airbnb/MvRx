package com.airbnb.mvrx.mocking

import android.content.Context
import com.airbnb.mvrx.*
import com.airbnb.mvrx.mocking.printer.MockPrinterConfiguration
import com.airbnb.mvrx.mocking.printer.MavericksMockPrinter
import com.airbnb.mvrx.mocking.printer.ViewModelStatePrinter

object MavericksMocks {
    /**
     * This global instance enables mock states to be forced onto ViewModels as they are created.
     * This enable easy testing.
     *
     * The instance is exposed publicly so references to mocked views can be cleared after a test
     * completes.
     */
    val mockStateHolder = MockStateHolder()

    /**
     * Configuration for how mock state is printed.
     *
     * The MvRx mocking system allows you to generate a reproduction of a ViewModel's state. For
     * any [MvRxState] instance that a ViewModel has, MvRx can generate a file containing code
     * to completely reconstruct that state.
     *
     * This generated code can then be used to reconstruct States that can be used during testing.
     * The scripts in the MvRx/mock_generation folder are used to interact with the device to pull
     * the resulting mock files.
     *
     * [enableMockPrinterBroadcastReceiver] must be enabled for this to work.
     *
     * See [MavericksMockPrinter]
     * See https://github.com/airbnb/MvRx/wiki/Mock-Printer
     */
    var mockPrinterConfiguration = MockPrinterConfiguration()

    /**
     * Calls to [MavericksMockPrinter.startReceiver] will be no-ops unless this is enabled.
     *
     * This will automatically be set when [install] is called.
     */
    var enableMockPrinterBroadcastReceiver: Boolean = false

    /**
     * Calls to [MockableMavericksView.provideMocks] will return empty unless this is enabled.
     *
     * This will automatically be set when [install] is called.
     */
    var enableMavericksViewMocking: Boolean = false

    val mockConfigFactory: MockMavericksViewModelConfigFactory
        get() {
            return (MvRx.viewModelConfigFactory as? MockMavericksViewModelConfigFactory)
                ?: error("Expecting MockMvRxViewModelConfigFactory for config factory. Make sure you have called MvRxMocks#install")
        }

    /**
     * If the application was built with the debuggable flag enabled in its Android Manifest then
     * this will add plugins to [MvRx] that enable working with mock State. This is useful for
     * both manual and automated testing of development builds.
     *
     * This function is a shortcut instead of setting each property in this object individually.
     * For custom control you can set properties directly instead.
     *
     * It is safe to call this in both debug and production
     * builds and it will take care of the correct behavior for you.
     *
     * The context will be used to automatically register a broadcast receiver for each
     * ViewModel created in the app with [ViewModelStatePrinter] so that the state printing
     * system is automatically enabled.
     *
     * Calling this subsequent times will replace the plugins with new instances.
     */
    fun install(context: Context) {
        val isDebuggable = context.isDebuggable()
        install(
                mocksEnabled = isDebuggable,
                debugMode = isDebuggable,
                context = context
        )
    }

    /**
     * Choose whether to enable [MvRx] mocking tools. This is useful for
     * both manual and automated testing of development builds.
     *
     * This function is a shortcut instead of setting each property in this object individually.
     * For custom control you can set properties directly instead.
     *
     * The context will be used to automatically register a broadcast receiver for each
     * ViewModel created in the app with [ViewModelStatePrinter] so that the state printing
     * system is automatically enabled.
     *
     * Calling this subsequent times will replace the plugins with new instances.
     *
     * @param debugMode True if debug checks should be enabled
     * @param mocksEnabled True if ViewModel mocking should be enabled.
     * @param context Application context. If provided this will be used to register a
     * [ViewModelStatePrinter] for each ViewModel to support mock state printing.
     */
    fun install(mocksEnabled: Boolean, debugMode: Boolean, context: Context?) {
        enableMockPrinterBroadcastReceiver = mocksEnabled
        enableMavericksViewMocking = mocksEnabled

        if (mocksEnabled) {
            val mockConfigFactory = MockMavericksViewModelConfigFactory(context?.applicationContext, debugMode)
            MvRx.viewModelConfigFactory = mockConfigFactory
            MvRx.viewModelDelegateFactory = MockViewModelDelegateFactory(mockConfigFactory)
        } else {
            // These are both set to make sure that all MvRx plugins are completely cleared
            // when debuggable is set to false. This helps in the unit testing case.
            MvRx.viewModelConfigFactory = MavericksViewModelConfigFactory(debugMode)
            MvRx.viewModelDelegateFactory = DefaultViewModelDelegateFactory()

        }
    }

    /**
     * If the given viewmodel has a state store that implements [ScriptableStateStore] then this
     * function can be used to set the next state via [ScriptableStateStore.next].
     *
     * It is an error to call this if the store is not scriptable.
     */
    fun <VM : BaseMavericksViewModel<S>, S : MvRxState> setScriptableState(viewModel: VM, state: S) {
        val stateStore = viewModel.config.stateStore
        check(stateStore is ScriptableStateStore) {
            "State store of ${viewModel.javaClass.simpleName} must be a ScriptableStateStore"
        }
        stateStore.next(state)
    }

    /**
     * A helper to set a state on a view model via [MvRxStateStore.set].
     *
     * This may not work if the ViewModel's state store is mocked or configured to not accept
     * state changes, and it is the responsibility of the caller to make sure that the state store
     * can accept changes.
     *
     * Additionally, it is the responsibility of the caller to understand the type of state store
     * the view model is using, and whether the state change will take affect synchronously or
     * asynchronously depending on the state store implementation.
     *
     * See [setScriptableState] if you want to force a state on a [ScriptableStateStore] that would
     * otherwise not allow state changes.
     */
    fun <VM : BaseMavericksViewModel<S>, S : MvRxState> setState(viewModel: VM, state: S) {
        val stateStore = viewModel.config.stateStore
        stateStore.set { state }
    }
}
