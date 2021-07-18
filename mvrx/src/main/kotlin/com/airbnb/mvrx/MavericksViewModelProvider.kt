package com.airbnb.mvrx

import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.io.Serializable

/**
 * Helper ViewModelProvider that has a single method for taking either a [Fragment] or [ComponentActivity] instead
 * of two separate ones. The logic for providing the correct scope is inside the method.
 */
@InternalMavericksApi
object MavericksViewModelProvider {
    /**
     * MvRx specific ViewModelProvider used for creating a BaseMavericksViewModel scoped to either a [Fragment] or [ComponentActivity].
     * If this is in a [Fragment], it cannot be called before the Fragment has been added to an Activity or wrapped in a [Lazy] call.
     *
     * @param viewModelClass The class of the ViewModel you would like an instance of.
     * @param stateClass The class of the State used by the ViewModel.
     * @param viewModelContext The [ViewModelContext] which contains arguments and the owner of the ViewModel.
     *                         Either [ActivityViewModelContext] or [FragmentViewModelContext].
     * @param key An optional key for the ViewModel in the store. This is optional but should be used if you have multiple of the same
     *            ViewModel class in the same scope.
     * @param forExistingViewModel If true the viewModel should already have been created. If it has not been created already,
     *                             a [ViewModelDoesNotExistException] will be thrown
     * @param initialStateFactory A way to specify how to create the initial state, can be mocked out for testing.
     *
     */
    fun <VM : MavericksViewModel<out S>, S : MavericksState> get(
        viewModelClass: Class<out VM>,
        stateClass: Class<out S>,
        viewModelContext: ViewModelContext,
        key: String = viewModelClass.name,
        forExistingViewModel: Boolean = false,
        initialStateFactory: MavericksStateFactory<VM, S> = RealMavericksStateFactory()
    ): VM {
        val savedStateRegistry = viewModelContext.savedStateRegistry

        if (!savedStateRegistry.isRestored) {
            error(ACCESSED_BEFORE_ON_CREATE_ERR_MSG)
        }

        val stateRestorer = savedStateRegistry
            .consumeRestoredStateForKey(key)
            ?.toStateRestorer<S>(viewModelContext)

        val restoredContext = stateRestorer?.viewModelContext ?: viewModelContext

        @Suppress("UNCHECKED_CAST")
        val viewModel: MavericksViewModelWrapper<VM, S> = ViewModelProvider(
            viewModelContext.owner,
            MavericksFactory(
                viewModelClass,
                stateClass,
                restoredContext,
                key,
                stateRestorer?.toRestoredState,
                forExistingViewModel,
                initialStateFactory
            )
        ).get(key, MavericksViewModelWrapper::class.java) as MavericksViewModelWrapper<VM, S>

        try {
            // Save the view model's state to the bundle so that it can be used to recreate
            // state across system initiated process death.
            viewModelContext.savedStateRegistry.registerSavedStateProvider(key) {
                viewModel.viewModel.getSavedStateBundle(restoredContext.args)
            }
        } catch (e: IllegalArgumentException) {
            // The view model was already registered with the context. We only want the initial
            // fragment that creates the view model to register with the saved state registry so
            // that it saves the correct arguments.
        }
        return viewModel.viewModel
    }

    private fun <VM : MavericksViewModel<out S>, S : MavericksState> VM.getSavedStateBundle(
        initialArgs: Any?
    ) = withState(this) { state ->
        Bundle().apply {
            putBundle(KEY_MVRX_SAVED_INSTANCE_STATE, state.persistState())
            initialArgs?.let { args ->
                when (args) {
                    is Parcelable -> putParcelable(KEY_MVRX_SAVED_ARGS, args)
                    is Serializable -> putSerializable(KEY_MVRX_SAVED_ARGS, args)
                    else -> error("Args must be parcelable or serializable")
                }
            }
        }
    }

    private fun <S : MavericksState> Bundle.toStateRestorer(viewModelContext: ViewModelContext): StateRestorer<S> {
        val restoredArgs = get(KEY_MVRX_SAVED_ARGS)
        val restoredState = getBundle(KEY_MVRX_SAVED_INSTANCE_STATE)

        requireNotNull(restoredState) { "State was not saved prior to restoring!" }

        val restoredContext = when (viewModelContext) {
            is ActivityViewModelContext -> viewModelContext.copy(args = restoredArgs)
            is FragmentViewModelContext -> viewModelContext.copy(args = restoredArgs)
        }
        return StateRestorer(restoredContext) { restoredState.restorePersistedState(it) }
    }

    private const val KEY_MVRX_SAVED_INSTANCE_STATE = "mvrx:saved_instance_state"
    private const val KEY_MVRX_SAVED_ARGS = "mvrx:saved_args"
}

/**
 * Return the [Class] of the companion [MavericksViewModelFactory] for a given ViewModel class, if it exists.
 */
internal fun <VM : MavericksViewModel<*>> Class<VM>.factoryCompanion(): Class<out MavericksViewModelFactory<VM, *>>? {
    return declaredClasses.firstOrNull {
        MavericksViewModelFactory::class.java.isAssignableFrom(it)
    }?.let { klass ->
        @Suppress("UNCHECKED_CAST")
        klass as Class<out MavericksViewModelFactory<VM, *>>
    }
}

/**
 * Given a companion class, use Java reflection to create an instance. This is used over
 * Kotlin reflection for performance.
 */
internal fun Class<*>.instance(): Any {
    return declaredConstructors.first { it.parameterTypes.size == 1 }.newInstance(null)
}

internal const val ACCESSED_BEFORE_ON_CREATE_ERR_MSG =
    "You can only access a view model after super.onCreate of your activity/fragment has been called."

private data class StateRestorer<S : MavericksState>(
    val viewModelContext: ViewModelContext,
    val toRestoredState: (S) -> S
)
