@file:Suppress("Detekt.ParameterListWrapping")

package com.airbnb.mvrx.mocking

import android.os.Parcelable
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.InternalMavericksApi
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.mocking.MavericksMock.Companion.DEFAULT_INITIALIZATION_NAME
import com.airbnb.mvrx.mocking.MavericksMock.Companion.DEFAULT_STATE_NAME
import com.airbnb.mvrx.mocking.MavericksMock.Companion.RESTORED_STATE_NAME
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Used with [MockableMavericksView.provideMocks] for mocking a Mavericks view that has no view models (eg only static content and arguments).
 *
 * @param defaultArgs If your view takes arguments you must provide an instance of those arguments here to be used as the default value for your mocks.
 *                      If your view has no arguments, pass null (and use Nothing as the type).
 *                      For example, if you use Fragments this would be the arguments that you initialize the Fragment with.
 *                      If you use the [Mavericks.KEY_ARG] key to pass a single Parcelable class to your Fragment you can pass that class here,
 *                      and it will automatically be wrapped in a Bundle with the [Mavericks.KEY_ARG] key. Otherwise you can pass a [Bundle] and
 *                      it will be passed along as-is to your Fragment when it is created.
 *
 * @param mockBuilder Optionally provide other argument variations via the [MockBuilder] DSL
 *
 * @see mockSingleViewModel
 */
fun <V : MockableMavericksView, Args : Parcelable> V.mockNoViewModels(
    defaultArgs: Args?,
    mockBuilder: MockBuilder<V, Args>.() -> Unit = {}
): MockBuilder<V, Args> = MockBuilder<V, Args>(defaultArgs).apply {
    mockBuilder()
    build(this@mockNoViewModels)
}

/**
 * Use this to split a Views's mocks into groups. Each group is defined with the normal mock
 * definition functions, and this function combines them.
 * This is helpful when mocks need different default states or arguments, so each group can have its own default.
 * Additionally, splitting mocks into groups allows tests to run faster as groups can be tested in parallel.
 *
 * @param mocks Each pair is a String description of the mock group, paired to the mock builder - ie ["Dog" to dogMocks()]. The string description
 * used to create the pair is prepended to the name of each mock in the group, so you can omit a reference to the group type in the individual mocks.
 */
inline fun <reified V : MockableMavericksView> V.combineMocks(
    vararg mocks: Pair<String, MavericksViewMocks<V, *>>
): MavericksViewMocks<V, *> = object : MavericksViewMocks<V, Parcelable>() {

    init {
        validate(V::class.simpleName!!)
    }

    override val mockGroups: List<List<MavericksMock<V, out Parcelable>>>
        get() {
            return mocks.map { (prefix, mocker) ->
                mocker.mocks.map {
                    MavericksMock(
                        name = "$prefix : ${it.name}",
                        argsProvider = it.argsProvider,
                        statesProvider = it.statesProvider,
                        forInitialization = it.forInitialization,
                        type = it.type
                    )
                }
            }
        }
}

/**
 * Define state values for a [MavericksView] that should be used in tests.
 * This is for use with [MockableMavericksView.provideMocks] when the view has a single view model.
 *
 * In the [mockBuilder] lambda you can use [MockBuilder.args] to define mock arguments that should be used to initialize the view and create the
 * initial view model state. Use [SingleViewModelMockBuilder.state] to define complete state objects.
 *
 * It is recommended that the "Default" mock state for a view represent the most canonical, complete
 * form of the View. For example, all data loaded, with no empty or null instances.
 *
 * Other mock variations should test alterations to this "default" state, with each variant testing
 * a minimal difference (ideally only one change). For example, a variant may test that a single
 * property is null or empty.
 *
 * This pattern is encouraged because:
 * 1. The tools for defining mock variations are designed to enable easy modification of the default
 * 2. Each mock variant is only one line of code, and is easily maintained
 * 3. Each variant tests a single edge case
 *
 * @param V The type of Mavericks view that is being mocked
 * @param viewModelReference A reference to the view model that will be mocked. Use the "::" operator for this - "MyFragment::myViewModel"
 * @param defaultState An instance of your State that represents the canonical version of your view. It will be the basis for your tests.
 * @param defaultArgs If your view takes arguments you must provide an instance of those arguments here to be used as the default value for your mocks.
 *                      If your view has no arguments, pass null (and use Nothing as the type).
 *                      For example, if you use Fragments this would be the arguments that you initialize the Fragment with.
 *                      If you use the [Mavericks.KEY_ARG] key to pass a single Parcelable class to your Fragment you can pass that class here,
 *                      and it will automatically be wrapped in a Bundle with the [Mavericks.KEY_ARG] key. Otherwise you can pass a [Bundle] and
 *                      it will be passed along as-is to your Fragment when it is created.
 *
 * @param mockBuilder A lambda where the [SingleViewModelMockBuilder] DSL can be used to specify additional mock variants.
 * @see mockTwoViewModels
 * @see mockNoViewModels
 */
fun <V : MockableMavericksView, Args : Parcelable, S : MavericksState> V.mockSingleViewModel(
    viewModelReference: KProperty1<V, MavericksViewModel<out S>>,
    defaultState: S,
    defaultArgs: Args?,
    mockBuilder: SingleViewModelMockBuilder<V, Args, S>.() -> Unit
): MockBuilder<V, Args> =
    SingleViewModelMockBuilder(viewModelReference, defaultState, defaultArgs).apply {
        mockBuilder()
        build(this@mockSingleViewModel)
    }

/**
 * Similar to [mockSingleViewModel], but for the two view model case.
 */
fun <V : MockableMavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    Args : Parcelable>
V.mockTwoViewModels(
    viewModel1Reference: KProperty1<V, VM1>,
    defaultState1: S1,
    viewModel2Reference: KProperty1<V, VM2>,
    defaultState2: S2,
    defaultArgs: Args?,
    mockBuilder: TwoViewModelMockBuilder<V, VM1, S1, VM2, S2, Args>.() -> Unit
): MockBuilder<V, Args> = TwoViewModelMockBuilder(
    viewModel1Reference,
    defaultState1,
    viewModel2Reference,
    defaultState2,
    defaultArgs
).apply {
    mockBuilder()
    build(this@mockTwoViewModels)
}

/**
 * Similar to [mockTwoViewModels], but for the three view model case.
 */
@Suppress("Detekt.ParameterListWrapping")
@SuppressWarnings("Detekt.LongParameterList")
fun <V : MockableMavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    Args : Parcelable>
V.mockThreeViewModels(
    viewModel1Reference: KProperty1<V, VM1>,
    defaultState1: S1,
    viewModel2Reference: KProperty1<V, VM2>,
    defaultState2: S2,
    viewModel3Reference: KProperty1<V, VM3>,
    defaultState3: S3,
    defaultArgs: Args?,
    mockBuilder: ThreeViewModelMockBuilder<V, VM1, S1, VM2, S2, VM3, S3, Args>.() -> Unit
): MockBuilder<V, Args> = ThreeViewModelMockBuilder(
    viewModel1Reference,
    defaultState1,
    viewModel2Reference,
    defaultState2,
    viewModel3Reference,
    defaultState3,
    defaultArgs
).apply {
    mockBuilder()
    build(this@mockThreeViewModels)
}

/**
 * Similar to [mockTwoViewModels], but for the four view model case.
 */
@Suppress("Detekt.ParameterListWrapping")
@SuppressWarnings("Detekt.LongParameterList")
fun <V : MockableMavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S4 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    Args : Parcelable>
V.mockFourViewModels(
    viewModel1Reference: KProperty1<V, VM1>,
    defaultState1: S1,
    viewModel2Reference: KProperty1<V, VM2>,
    defaultState2: S2,
    viewModel3Reference: KProperty1<V, VM3>,
    defaultState3: S3,
    viewModel4Reference: KProperty1<V, VM4>,
    defaultState4: S4,
    defaultArgs: Args?,
    mockBuilder: FourViewModelMockBuilder<V, VM1, S1, VM2, S2, VM3, S3, VM4, S4, Args>.() -> Unit
): MockBuilder<V, Args> = FourViewModelMockBuilder(
    viewModel1Reference,
    defaultState1,
    viewModel2Reference,
    defaultState2,
    viewModel3Reference,
    defaultState3,
    viewModel4Reference,
    defaultState4,
    defaultArgs
).apply {
    mockBuilder()
    build(this@mockFourViewModels)
}

/**
 * Similar to [mockTwoViewModels], but for the five view model case.
 */
@Suppress("Detekt.ParameterListWrapping")
@SuppressWarnings("Detekt.LongParameterList")
fun <V : MockableMavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S4 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S5 : MavericksState,
    VM5 : MavericksViewModel<out S5>,
    Args : Parcelable>
V.mockFiveViewModels(
    viewModel1Reference: KProperty1<V, VM1>,
    defaultState1: S1,
    viewModel2Reference: KProperty1<V, VM2>,
    defaultState2: S2,
    viewModel3Reference: KProperty1<V, VM3>,
    defaultState3: S3,
    viewModel4Reference: KProperty1<V, VM4>,
    defaultState4: S4,
    viewModel5Reference: KProperty1<V, VM5>,
    defaultState5: S5,
    defaultArgs: Args?,
    mockBuilder: FiveViewModelMockBuilder<V, VM1, S1, VM2, S2, VM3, S3, VM4, S4, VM5, S5, Args>.() -> Unit
): MockBuilder<V, Args> = FiveViewModelMockBuilder(
    viewModel1Reference,
    defaultState1,
    viewModel2Reference,
    defaultState2,
    viewModel3Reference,
    defaultState3,
    viewModel4Reference,
    defaultState4,
    viewModel5Reference,
    defaultState5,
    defaultArgs
).apply {
    mockBuilder()
    build(this@mockFiveViewModels)
}

@Suppress("Detekt.ParameterListWrapping")
@SuppressWarnings("Detekt.LongParameterList")
fun <V : MockableMavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S4 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S5 : MavericksState,
    VM5 : MavericksViewModel<out S5>,
    S6 : MavericksState,
    VM6 : MavericksViewModel<out S6>,
    Args : Parcelable>
V.mockSixViewModels(
    viewModel1Reference: KProperty1<V, VM1>,
    defaultState1: S1,
    viewModel2Reference: KProperty1<V, VM2>,
    defaultState2: S2,
    viewModel3Reference: KProperty1<V, VM3>,
    defaultState3: S3,
    viewModel4Reference: KProperty1<V, VM4>,
    defaultState4: S4,
    viewModel5Reference: KProperty1<V, VM5>,
    defaultState5: S5,
    viewModel6Reference: KProperty1<V, VM6>,
    defaultState6: S6,
    defaultArgs: Args?,
    mockBuilder: SixViewModelMockBuilder<V, VM1, S1, VM2, S2, VM3, S3, VM4, S4, VM5, S5, VM6, S6, Args>.() -> Unit
): MockBuilder<V, Args> = SixViewModelMockBuilder(
    viewModel1Reference,
    defaultState1,
    viewModel2Reference,
    defaultState2,
    viewModel3Reference,
    defaultState3,
    viewModel4Reference,
    defaultState4,
    viewModel5Reference,
    defaultState5,
    viewModel6Reference,
    defaultState6,
    defaultArgs
).apply {
    mockBuilder()
    build(this@mockSixViewModels)
}

@Suppress("Detekt.ParameterListWrapping")
@SuppressWarnings("Detekt.LongParameterList")
fun <V : MockableMavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S4 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S5 : MavericksState,
    VM5 : MavericksViewModel<out S5>,
    S6 : MavericksState,
    VM6 : MavericksViewModel<out S6>,
    S7 : MavericksState,
    VM7 : MavericksViewModel<out S7>,
    Args : Parcelable>
V.mockSevenViewModels(
    viewModel1Reference: KProperty1<V, VM1>,
    defaultState1: S1,
    viewModel2Reference: KProperty1<V, VM2>,
    defaultState2: S2,
    viewModel3Reference: KProperty1<V, VM3>,
    defaultState3: S3,
    viewModel4Reference: KProperty1<V, VM4>,
    defaultState4: S4,
    viewModel5Reference: KProperty1<V, VM5>,
    defaultState5: S5,
    viewModel6Reference: KProperty1<V, VM6>,
    defaultState6: S6,
    viewModel7Reference: KProperty1<V, VM7>,
    defaultState7: S7,
    defaultArgs: Args?,
    mockBuilder: SevenViewModelMockBuilder<V, VM1, S1, VM2, S2, VM3, S3, VM4, S4, VM5, S5, VM6, S6, VM7, S7, Args>.() -> Unit
): MockBuilder<V, Args> = SevenViewModelMockBuilder(
    viewModel1Reference,
    defaultState1,
    viewModel2Reference,
    defaultState2,
    viewModel3Reference,
    defaultState3,
    viewModel4Reference,
    defaultState4,
    viewModel5Reference,
    defaultState5,
    viewModel6Reference,
    defaultState6,
    viewModel7Reference,
    defaultState7,
    defaultArgs
).apply {
    mockBuilder()
    build(this@mockSevenViewModels)
}

/**
 * Defines a unique variation of a View's state for testing purposes.
 *
 * A view is represented completely by:
 * 1. The arguments used to initialize it
 * 2. The State classes of all ViewModels used by the View
 *
 * For proper mocking, the View MUST NOT reference data from any other sources, such as static
 * singletons, dependency injection, shared preferences, etc. All of this should be channeled
 * through the ViewModel and State. Otherwise a mock cannot deterministically and completely
 * be used to test the View.
 *
 * An exception is Android OS level View state, with things such as scroll and cursor positions.
 * These are not feasible to track in state, and are generally independent and inconsequential in
 * testing.
 *
 * It is recommended that the "Default" mock state for a view represent the most canonical, complete
 * form of the View. For example, all data loaded, with no empty or null instances.
 *
 * Other mock variations should test alterations to this "default" state, with each variant testing
 * a minimal difference (ideally only one change). For example, a variant may test that a single
 * property is null or empty.
 *
 * This pattern is encouraged because:
 * 1. The tools for defining mock variations are designed to enable easy modification of the default
 * 2. Each mock variant is only one line of code, and is easily maintained
 * 3. Each variant tests a single edge case
 */
class MavericksMock<V : MavericksView, Args : Parcelable> @PublishedApi internal constructor(
    val name: String,
    @PublishedApi internal val argsProvider: () -> Args?,
    @PublishedApi internal val statesProvider: () -> List<MockState<V, *>>,
    /**
     * If true, this mock tests the view being created either from arguments or with the viewmodels'
     * default constructor (when [args] is null).
     */
    val forInitialization: Boolean = false,
    val type: Type = Type.Custom
) {

    /**
     * Returns the arguments that should be used to initialize the Mavericks view. If null, the view models will be
     * initialized purely with the mock states instead.
     */
    val args: Args? by lazy { argsProvider() }

    /**
     * The State to set on each ViewModel in the View. There should be a one to one match.
     */
    val states: List<MockState<V, *>> by lazy { statesProvider() }

    /**
     * Find a mocked state value for the given view model property.
     *
     * If null is returned it means the initial state should be created through the
     * default mavericks mechanism of arguments.
     */
    fun stateForViewModelProperty(property: KProperty<*>, existingViewModel: Boolean): MavericksState? {
        if (forInitialization && !existingViewModel) {
            // In the multi viewmodel case, an "existing" view model needs to be mocked with initial state (since it's args
            // would be from a different view), but for viewmodels being created in this view we can use args to create initial state.
            return null
        }

        // It's possible to have multiple viewmodels of the same type within a fragment. To differentiate them we look
        // at the property names.
        val viewModelToUse = states.firstOrNull { it.viewModelProperty.name == property.name }
        return viewModelToUse?.state
            ?: error("No state found for ViewModel property '${property.name}'. Available view models are ${states.map { it.viewModelProperty.name }}")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MavericksMock<*, *>

        if (name != other.name) return false
        if (forInitialization != other.forInitialization) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + forInitialization.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    val isDefaultInitialization: Boolean get() = type == Type.DefaultInitialization
    val isDefaultState: Boolean get() = type == Type.DefaultState
    val isForProcessRecreation: Boolean get() = type == Type.ProcessRecreation

    /**
     * There are a set of standard mock variants that all Views have. Beyond that users can define
     * custom mock variants.
     */
    enum class Type {
        /** Uses view arguments to test the initialization pathway of Mavericks. */
        DefaultInitialization,

        /** Uses the default state of a mock builder to represent the canonical form of a View. */
        DefaultState,

        /** Takes the default state and applies the process used to save and restore state. The output
         * state approximates the State after app process recreation. (This cannot be exact because
         * in practice it may rely on arguments set in a previous Fragment.)
         */
        ProcessRecreation,

        /** Any additional mock variant defined by the user. */
        Custom
    }

    companion object {
        internal const val DEFAULT_INITIALIZATION_NAME = "Default initialization"
        internal const val DEFAULT_STATE_NAME = "Default state"
        internal const val RESTORED_STATE_NAME = "Default state after process recreation"
    }
}

/**
 * A mocked State value and a reference to the ViewModel that the State is intended for.
 */
data class MockState<V : MavericksView, S : MavericksState> internal constructor(
    val viewModelProperty: KProperty1<V, MavericksViewModel<out S>>,
    val state: S
)

/**
 * Provides a DSL for defining variations to the default mock state.
 */
class SingleViewModelMockBuilder<V : MockableMavericksView, Args : Parcelable, S : MavericksState> internal constructor(
    private val viewModelReference: KProperty1<V, MavericksViewModel<out S>>,
    private val defaultState: S,
    defaultArgs: Args?
) : MockBuilder<V, Args>(defaultArgs, viewModelReference.pairDefault(defaultState)) {

    /**
     * Provide a state object via the lambda for the view model being mocked.
     * The receiver of the lambda is the default state provided in the top level mock method. For simplicity you
     * can modify the receiver directly.
     *
     * The DSL provided by [DataClassSetDsl] can be used for simpler state modification.
     *
     * Mock variations should test alterations to the "default" state, with each variant testing
     * a minimal difference (ideally only one change). For example, a variant may test that a single
     * property is null or empty.
     *
     * @param name Describes the UI the state puts the view in. Should be unique.
     * @param args The arguments that should be provided to the view.
     *             This is only useful if the view accesses arguments directly to get data that is not provided in the view model state.
     *             In other cases it should be omitted. By default the args you set as default in the top level mock method will be used.
     *             The receiver of the lambda is the default args.
     * @param stateBuilder A lambda whose return object is the state for this mock. The lambda receiver is the default state.
     */
    fun state(name: String, args: (Args.() -> Args)? = null, stateBuilder: S.() -> S) {
        addState(
            name = name,
            argsProvider = evaluateArgsLambda(args),
            statesProvider = { listOf(MockState(viewModelReference, defaultState.stateBuilder())) }
        )
    }

    /**
     * Helper to mock the loading and failure state of an Async property on your state.
     * Creates two different mocked states stemmed from the given state - one where the async property is set to Loading
     * and one where it is set to Fail.
     */
    fun <T, A : Async<T>> stateForLoadingAndFailure(
        state: S = defaultState,
        asyncPropertyBlock: S.() -> KProperty0<A>
    ) {
        val asyncProperty = state.asyncPropertyBlock()
        // Split "myProperty" to "My property"
        val asyncName =
            asyncProperty.name.replace(Regex("[A-Z]")) { " ${it.value.toLowerCase()}" }.trim()
                .capitalize()

        state("$asyncName loading") {
            state.setLoading { asyncProperty }
        }

        state("$asyncName failed") {
            state.setNetworkFailure { asyncProperty }
        }
    }
}

private fun <V : MockableMavericksView, S : MavericksState, VM : MavericksViewModel<out S>> KProperty1<V, VM>.pairDefault(
    state: MavericksState
): Pair<KProperty1<V, MavericksViewModel<MavericksState>>, MavericksState> {
    @Suppress("UNCHECKED_CAST")
    return (this to state) as Pair<KProperty1<V, MavericksViewModel<MavericksState>>, MavericksState>
}

class TwoViewModelMockBuilder<
    V : MockableMavericksView,
    VM1 : MavericksViewModel<out S1>,
    S1 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S2 : MavericksState,
    Args : Parcelable>
internal constructor(
    val vm1: KProperty1<V, VM1>,
    val defaultState1: S1,
    val vm2: KProperty1<V, VM2>,
    val defaultState2: S2,
    defaultArgs: Args?
) : MockBuilder<V, Args>(
    defaultArgs,
    vm1.pairDefault(defaultState1),
    vm2.pairDefault(defaultState2)
) {

    /**
     * Provide state objects for each view model in the view.
     *
     * The DSL provided by [DataClassSetDsl] can be used for simpler state modification.
     *
     * Mock variations should test alterations to the "default" state, with each variant testing
     * a minimal difference (ideally only one change). For example, a variant may test that a single
     * property is null or empty.
     *
     * @param name Describes the UI these states put the view in. Should be unique.
     * @param args The arguments that should be provided to the view.
     *             This is only useful if the view accesses arguments directly to get data that is not provided in the view model state.
     *             In other cases it should be omitted. By default the args you set as default in the top level mock method will be used.
     *             The receiver of the lambda is the default args.
     * @param statesBuilder A lambda that is used to define state objects for each view model.
     */
    fun state(
        name: String,
        args: (Args.() -> Args)? = null,
        statesBuilder: TwoStatesBuilder<V, S1, VM1, S2, VM2>.() -> Unit
    ) {
        addState(
            name, evaluateArgsLambda(args),
            {
                TwoStatesBuilder(
                    vm1,
                    defaultState1,
                    vm2,
                    defaultState2
                ).apply(statesBuilder).states
            }
        )
    }

    /**
     * Helper to mock the loading and failure state of an Async property on your state in the first view model.
     * Creates two different mocked states stemmed from the given state - one where the async property is set to Loading
     * and one where it is set to Fail.
     */
    fun <T, A : Async<T>> viewModel1StateForLoadingAndFailure(
        state: S1 = defaultState1,
        asyncPropertyBlock: S1.() -> KProperty0<A>
    ) {
        val asyncProperty = state.asyncPropertyBlock()
        val asyncName = asyncProperty.splitNameByCase()

        state("$asyncName loading") {
            viewModel1 {
                state.setLoading { asyncProperty }
            }
        }

        state("$asyncName failed") {
            viewModel1 {
                state.setNetworkFailure { asyncProperty }
            }
        }
    }

    /**
     * Helper to mock the loading and failure state of an Async property on your state in the second view model.
     * Creates two different mocked states stemmed from the given state - one where the async property is set to Loading
     * and one where it is set to Fail.
     */
    fun <T, A : Async<T>> viewModel2StateForLoadingAndFailure(
        state: S2 = defaultState2,
        asyncPropertyBlock: S2.() -> KProperty0<A>
    ) {
        val asyncProperty = state.asyncPropertyBlock()

        val asyncName = asyncProperty.splitNameByCase()

        state("$asyncName loading") {
            viewModel2 {
                state.setLoading { asyncProperty }
            }
        }

        state("$asyncName failed") {
            viewModel2 {
                state.setNetworkFailure { asyncProperty }
            }
        }
    }
}

// Split "myProperty" to "My property"
private fun KProperty0<Any?>.splitNameByCase(): String {
    return name.replace(Regex("[A-Z]")) { " ${it.value.toLowerCase()}" }.trim().capitalize()
}

/**
 * Helper to provide mock state definitions for multiple view models.
 *
 * Usage is like:
 *
 * viewModel1 {
 *   // receiver is default state, make change and return new state
 * }
 *
 * viewModel2 {
 *   // receiver is default state, make change and return new state
 * }
 */
open class TwoStatesBuilder<
    V : MavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>>
internal constructor(
    val vm1: KProperty1<V, VM1>,
    val defaultState1: S1,
    val vm2: KProperty1<V, VM2>,
    val defaultState2: S2
) {
    private val stateMap = mutableMapOf<KProperty1<V, MavericksViewModel<MavericksState>>, MavericksState>()

    internal val states: List<MockState<V, *>>
        get() = stateMap.map { entry ->
            MockState(
                entry.key,
                entry.value
            )
        }

    protected infix fun <VM : MavericksViewModel<out S>, S : MavericksState> KProperty1<V, VM>.setStateTo(
        state: S
    ) {
        @Suppress("UNCHECKED_CAST")
        stateMap[this as KProperty1<V, MavericksViewModel<MavericksState>>] = state
    }

    init {
        vm1 setStateTo defaultState1
        vm2 setStateTo defaultState2
    }

    /**
     * Define a state to be used when mocking your first view model (as defined in the top level mock method).
     * If this method isn't called, your default state will be used automatically.
     * For convenience, the receiver of the lambda is the default state.
     */
    fun viewModel1(stateBuilder: S1.() -> S1) {
        vm1 setStateTo defaultState1.stateBuilder()
    }

    /**
     * Define a state to be used when mocking your second view model (as defined in the top level mock method).
     * If this method isn't called, your default state will be used automatically.
     * For convenience, the receiver of the lambda is the default state.
     */
    fun viewModel2(stateBuilder: S2.() -> S2) {
        vm2 setStateTo defaultState2.stateBuilder()
    }
}

class ThreeViewModelMockBuilder<
    V : MockableMavericksView,
    VM1 : MavericksViewModel<out S1>,
    S1 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S2 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S3 : MavericksState,
    Args : Parcelable>
internal constructor(
    val vm1: KProperty1<V, VM1>,
    val defaultState1: S1,
    val vm2: KProperty1<V, VM2>,
    val defaultState2: S2,
    val vm3: KProperty1<V, VM3>,
    val defaultState3: S3,
    defaultArgs: Args?
) : MockBuilder<V, Args>(
    defaultArgs,
    vm1.pairDefault(defaultState1),
    vm2.pairDefault(defaultState2),
    vm3.pairDefault(defaultState3)
) {

    /**
     * Provide state objects for each view model in the view.
     *
     * @param name Describes the UI these states put the view in. Should be unique.
     * @param args The arguments that should be provided to the view.
     *             This is only used if the view accesses arguments directly to get data that is not provided in the view model state.
     *             In other cases it should be omitted. This must be provided if the view accesses args directly.
     * @param statesBuilder A lambda that is used to define state objects for each view model. See [ThreeStatesBuilder]
     */
    fun state(
        name: String,
        args: (Args.() -> Args)? = null,
        statesBuilder: ThreeStatesBuilder<V, S1, VM1, S2, VM2, S3, VM3>.() -> Unit
    ) {
        addState(
            name, evaluateArgsLambda(args),
            {
                ThreeStatesBuilder(
                    vm1,
                    defaultState1,
                    vm2,
                    defaultState2,
                    vm3,
                    defaultState3
                ).apply(statesBuilder).states
            }
        )
    }

    /**
     * Helper to mock the loading and failure state of an Async property on your state in the first view model.
     * Creates two different mocked states stemmed from the given state - one where the async property is set to Loading
     * and one where it is set to Fail.
     */
    fun <T, A : Async<T>> viewModel1StateForLoadingAndFailure(
        state: S1 = defaultState1,
        asyncPropertyBlock: S1.() -> KProperty0<A>
    ) {
        val asyncProperty = state.asyncPropertyBlock()
        val asyncName = asyncProperty.splitNameByCase()

        state("$asyncName loading") {
            viewModel1 {
                state.setLoading { asyncProperty }
            }
        }

        state("$asyncName failed") {
            viewModel1 {
                state.setNetworkFailure { asyncProperty }
            }
        }
    }

    /**
     * Helper to mock the loading and failure state of an Async property on your state in the second view model.
     * Creates two different mocked states stemmed from the given state - one where the async property is set to Loading
     * and one where it is set to Fail.
     */
    fun <T, A : Async<T>> viewModel2StateForLoadingAndFailure(
        state: S2 = defaultState2,
        asyncPropertyBlock: S2.() -> KProperty0<A>
    ) {
        val asyncProperty = state.asyncPropertyBlock()

        val asyncName = asyncProperty.splitNameByCase()

        state("$asyncName loading") {
            viewModel2 {
                state.setLoading { asyncProperty }
            }
        }

        state("$asyncName failed") {
            viewModel2 {
                state.setNetworkFailure { asyncProperty }
            }
        }
    }

    /**
     * Helper to mock the loading and failure state of an Async property on your state in the third view model.
     * Creates two different mocked states stemmed from the given state - one where the async property is set to Loading
     * and one where it is set to Fail.
     */
    fun <T, A : Async<T>> viewModel3StateForLoadingAndFailure(
        state: S3 = defaultState3,
        asyncPropertyBlock: S3.() -> KProperty0<A>
    ) {
        val asyncProperty = state.asyncPropertyBlock()

        val asyncName = asyncProperty.splitNameByCase()

        state("$asyncName loading") {
            viewModel3 {
                state.setLoading { asyncProperty }
            }
        }

        state("$asyncName failed") {
            viewModel3 {
                state.setNetworkFailure { asyncProperty }
            }
        }
    }
}

open class ThreeStatesBuilder<
    V : MavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>>
internal constructor(
    vm1: KProperty1<V, VM1>,
    defaultState1: S1,
    vm2: KProperty1<V, VM2>,
    defaultState2: S2,
    val vm3: KProperty1<V, VM3>,
    val defaultState3: S3
) : TwoStatesBuilder<V, S1, VM1, S2, VM2>(vm1, defaultState1, vm2, defaultState2) {

    init {
        vm3 setStateTo defaultState3
    }

    /**
     * Define a state to be used when mocking your third view model (as defined in the top level mock method).
     * If this method isn't called, your default state will be used automatically.
     * For convenience, the receiver of the lambda is the default state.
     */
    fun viewModel3(stateBuilder: S3.() -> S3) {
        vm3 setStateTo defaultState3.stateBuilder()
    }
}

class FourViewModelMockBuilder<
    V : MockableMavericksView,
    VM1 : MavericksViewModel<out S1>,
    S1 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S2 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S3 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S4 : MavericksState,
    Args : Parcelable>
internal constructor(
    val vm1: KProperty1<V, VM1>,
    val defaultState1: S1,
    val vm2: KProperty1<V, VM2>,
    val defaultState2: S2,
    val vm3: KProperty1<V, VM3>,
    val defaultState3: S3,
    val vm4: KProperty1<V, VM4>,
    val defaultState4: S4,
    defaultArgs: Args?
) : MockBuilder<V, Args>(
    defaultArgs,
    vm1.pairDefault(defaultState1),
    vm2.pairDefault(defaultState2),
    vm3.pairDefault(defaultState3),
    vm4.pairDefault(defaultState4)
) {

    /**
     * Provide state objects for each view model in the view.
     *
     * @param name Describes the UI these states put the view in. Should be unique.
     * @param args The arguments that should be provided to the view.
     *             This is only used if the view accesses arguments directly to get data that is not provided in the view model state.
     *             In other cases it should be omitted. This must be provided if the view accesses args directly.
     * @param statesBuilder A lambda that is used to define state objects for each view model. See [FourStatesBuilder]
     */
    fun state(
        name: String,
        args: (Args.() -> Args)? = null,
        statesBuilder: FourStatesBuilder<V, S1, VM1, S2, VM2, S3, VM3, S4, VM4>.() -> Unit
    ) {
        addState(
            name,
            evaluateArgsLambda(args),
            {
                FourStatesBuilder(
                    vm1,
                    defaultState1,
                    vm2,
                    defaultState2,
                    vm3,
                    defaultState3,
                    vm4,
                    defaultState4
                ).apply(statesBuilder).states
            }
        )
    }
}

open class FourStatesBuilder<
    V : MavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S4 : MavericksState,
    VM4 : MavericksViewModel<out S4>>
internal constructor(
    vm1: KProperty1<V, VM1>,
    defaultState1: S1,
    vm2: KProperty1<V, VM2>,
    defaultState2: S2,
    vm3: KProperty1<V, VM3>,
    defaultState3: S3,
    val vm4: KProperty1<V, VM4>,
    val defaultState4: S4
) : ThreeStatesBuilder<V, S1, VM1, S2, VM2, S3, VM3>(
    vm1,
    defaultState1,
    vm2,
    defaultState2,
    vm3,
    defaultState3
) {

    init {
        vm4 setStateTo defaultState4
    }

    /**
     * Define a state to be used when mocking your fourth view model (as defined in the top level mock method).
     * If this method isn't called, your default state will be used automatically.
     * For convenience, the receiver of the lambda is the default state.
     */
    fun viewModel4(stateBuilder: S4.() -> S4) {
        vm4 setStateTo defaultState4.stateBuilder()
    }
}

class FiveViewModelMockBuilder<
    V : MockableMavericksView,
    VM1 : MavericksViewModel<out S1>,
    S1 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S2 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S3 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S4 : MavericksState,
    VM5 : MavericksViewModel<out S5>,
    S5 : MavericksState,
    Args : Parcelable>
internal constructor(
    val vm1: KProperty1<V, VM1>,
    val defaultState1: S1,
    val vm2: KProperty1<V, VM2>,
    val defaultState2: S2,
    val vm3: KProperty1<V, VM3>,
    val defaultState3: S3,
    val vm4: KProperty1<V, VM4>,
    val defaultState4: S4,
    val vm5: KProperty1<V, VM5>,
    val defaultState5: S5,
    defaultArgs: Args?
) : MockBuilder<V, Args>(
    defaultArgs,
    vm1.pairDefault(defaultState1),
    vm2.pairDefault(defaultState2),
    vm3.pairDefault(defaultState3),
    vm4.pairDefault(defaultState4),
    vm5.pairDefault(defaultState5)
) {

    /**
     * Provide state objects for each view model in the view.
     *
     * @param name Describes the UI these states put the view in. Should be unique.
     * @param args The arguments that should be provided to the view.
     *             This is only used if the view accesses arguments directly to get data that is not provided in the view model state.
     *             In other cases it should be omitted. This must be provided if the view accesses args directly.
     * @param statesBuilder A lambda that is used to define state objects for each view model. See [FiveStatesBuilder]
     */
    fun state(
        name: String,
        args: (Args.() -> Args)? = null,
        statesBuilder: FiveStatesBuilder<V, S1, VM1, S2, VM2, S3, VM3, S4, VM4, S5, VM5>.() -> Unit
    ) {
        addState(
            name,
            evaluateArgsLambda(args),
            {
                FiveStatesBuilder(
                    vm1,
                    defaultState1,
                    vm2,
                    defaultState2,
                    vm3,
                    defaultState3,
                    vm4,
                    defaultState4,
                    vm5,
                    defaultState5
                ).apply(statesBuilder).states
            }
        )
    }
}

open class FiveStatesBuilder<
    V : MavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S4 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S5 : MavericksState,
    VM5 : MavericksViewModel<out S5>>
internal constructor(
    vm1: KProperty1<V, VM1>,
    defaultState1: S1,
    vm2: KProperty1<V, VM2>,
    defaultState2: S2,
    vm3: KProperty1<V, VM3>,
    defaultState3: S3,
    vm4: KProperty1<V, VM4>,
    defaultState4: S4,
    val vm5: KProperty1<V, VM5>,
    val defaultState5: S5
) : FourStatesBuilder<V, S1, VM1, S2, VM2, S3, VM3, S4, VM4>(
    vm1,
    defaultState1,
    vm2,
    defaultState2,
    vm3,
    defaultState3,
    vm4,
    defaultState4
) {

    init {
        vm5 setStateTo defaultState5
    }

    /**
     * Define a state to be used when mocking your fifth view model (as defined in the top level mock method).
     * If this method isn't called, your default state will be used automatically.
     * For convenience, the receiver of the lambda is the default state.
     */
    fun viewModel5(stateBuilder: S5.() -> S5) {
        vm5 setStateTo defaultState5.stateBuilder()
    }
}

class SixViewModelMockBuilder<
    V : MockableMavericksView,
    VM1 : MavericksViewModel<out S1>,
    S1 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S2 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S3 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S4 : MavericksState,
    VM5 : MavericksViewModel<out S5>,
    S5 : MavericksState,
    VM6 : MavericksViewModel<out S6>,
    S6 : MavericksState,
    Args : Parcelable>
internal constructor(
    val vm1: KProperty1<V, VM1>,
    val defaultState1: S1,
    val vm2: KProperty1<V, VM2>,
    val defaultState2: S2,
    val vm3: KProperty1<V, VM3>,
    val defaultState3: S3,
    val vm4: KProperty1<V, VM4>,
    val defaultState4: S4,
    val vm5: KProperty1<V, VM5>,
    val defaultState5: S5,
    val vm6: KProperty1<V, VM6>,
    val defaultState6: S6,
    defaultArgs: Args?
) : MockBuilder<V, Args>(
    defaultArgs,
    vm1.pairDefault(defaultState1),
    vm2.pairDefault(defaultState2),
    vm3.pairDefault(defaultState3),
    vm4.pairDefault(defaultState4),
    vm5.pairDefault(defaultState5),
    vm6.pairDefault(defaultState6)
) {

    /**
     * Provide state objects for each view model in the view.
     *
     * @param name Describes the UI these states put the view in. Should be unique.
     * @param args The arguments that should be provided to the view.
     *             This is only used if the view accesses arguments directly to get data that is not provided in the view model state.
     *             In other cases it should be omitted. This must be provided if the view accesses args directly.
     * @param statesBuilder A lambda that is used to define state objects for each view model. See [SixStatesBuilder]
     */
    fun state(
        name: String,
        args: (Args.() -> Args)? = null,
        statesBuilder: SixStatesBuilder<V, S1, VM1, S2, VM2, S3, VM3, S4, VM4, S5, VM5, S6, VM6>.() -> Unit
    ) {
        addState(
            name,
            evaluateArgsLambda(args),
            {
                SixStatesBuilder(
                    vm1,
                    defaultState1,
                    vm2,
                    defaultState2,
                    vm3,
                    defaultState3,
                    vm4,
                    defaultState4,
                    vm5,
                    defaultState5,
                    vm6,
                    defaultState6
                ).apply(statesBuilder).states
            }
        )
    }
}

open class SixStatesBuilder<
    V : MavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S4 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S5 : MavericksState,
    VM5 : MavericksViewModel<out S5>,
    S6 : MavericksState,
    VM6 : MavericksViewModel<out S6>>
internal constructor(
    vm1: KProperty1<V, VM1>,
    defaultState1: S1,
    vm2: KProperty1<V, VM2>,
    defaultState2: S2,
    vm3: KProperty1<V, VM3>,
    defaultState3: S3,
    vm4: KProperty1<V, VM4>,
    defaultState4: S4,
    vm5: KProperty1<V, VM5>,
    defaultState5: S5,
    val vm6: KProperty1<V, VM6>,
    val defaultState6: S6
) : FiveStatesBuilder<V, S1, VM1, S2, VM2, S3, VM3, S4, VM4, S5, VM5>(
    vm1,
    defaultState1,
    vm2,
    defaultState2,
    vm3,
    defaultState3,
    vm4,
    defaultState4,
    vm5,
    defaultState5
) {

    init {
        vm6 setStateTo defaultState6
    }

    /**
     * Define a state to be used when mocking your sixth view model (as defined in the top level mock method).
     * If this method isn't called, your default state will be used automatically.
     * For convenience, the receiver of the lambda is the default state.
     */
    fun viewModel6(stateBuilder: S6.() -> S6) {
        vm6 setStateTo defaultState6.stateBuilder()
    }
}

class SevenViewModelMockBuilder<
    V : MockableMavericksView,
    VM1 : MavericksViewModel<out S1>,
    S1 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S2 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S3 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S4 : MavericksState,
    VM5 : MavericksViewModel<out S5>,
    S5 : MavericksState,
    VM6 : MavericksViewModel<out S6>,
    S6 : MavericksState,
    VM7 : MavericksViewModel<out S7>,
    S7 : MavericksState,
    Args : Parcelable>
internal constructor(
    val vm1: KProperty1<V, VM1>,
    val defaultState1: S1,
    val vm2: KProperty1<V, VM2>,
    val defaultState2: S2,
    val vm3: KProperty1<V, VM3>,
    val defaultState3: S3,
    val vm4: KProperty1<V, VM4>,
    val defaultState4: S4,
    val vm5: KProperty1<V, VM5>,
    val defaultState5: S5,
    val vm6: KProperty1<V, VM6>,
    val defaultState6: S6,
    val vm7: KProperty1<V, VM7>,
    val defaultState7: S7,
    defaultArgs: Args?
) : MockBuilder<V, Args>(
    defaultArgs,
    vm1.pairDefault(defaultState1),
    vm2.pairDefault(defaultState2),
    vm3.pairDefault(defaultState3),
    vm4.pairDefault(defaultState4),
    vm5.pairDefault(defaultState5),
    vm6.pairDefault(defaultState6),
    vm7.pairDefault(defaultState7)
) {

    /**
     * Provide state objects for each view model in the view.
     *
     * @param name Describes the UI these states put the view in. Should be unique.
     * @param args The arguments that should be provided to the view.
     *             This is only used if the view accesses arguments directly to get data that is not provided in the view model state.
     *             In other cases it should be omitted. This must be provided if the view accesses args directly.
     * @param statesBuilder A lambda that is used to define state objects for each view model. See [SevenStatesBuilder]
     */
    fun state(
        name: String,
        args: (Args.() -> Args)? = null,
        statesBuilder: SevenStatesBuilder<V, S1, VM1, S2, VM2, S3, VM3, S4, VM4, S5, VM5, S6, VM6, S7, VM7>.() -> Unit
    ) {
        addState(
            name,
            evaluateArgsLambda(args),
            {
                SevenStatesBuilder(
                    vm1,
                    defaultState1,
                    vm2,
                    defaultState2,
                    vm3,
                    defaultState3,
                    vm4,
                    defaultState4,
                    vm5,
                    defaultState5,
                    vm6,
                    defaultState6,
                    vm7,
                    defaultState7
                ).apply(statesBuilder).states
            }
        )
    }
}

open class SevenStatesBuilder<
    V : MavericksView,
    S1 : MavericksState,
    VM1 : MavericksViewModel<out S1>,
    S2 : MavericksState,
    VM2 : MavericksViewModel<out S2>,
    S3 : MavericksState,
    VM3 : MavericksViewModel<out S3>,
    S4 : MavericksState,
    VM4 : MavericksViewModel<out S4>,
    S5 : MavericksState,
    VM5 : MavericksViewModel<out S5>,
    S6 : MavericksState,
    VM6 : MavericksViewModel<out S6>,
    S7 : MavericksState,
    VM7 : MavericksViewModel<out S7>>
internal constructor(
    vm1: KProperty1<V, VM1>,
    defaultState1: S1,
    vm2: KProperty1<V, VM2>,
    defaultState2: S2,
    vm3: KProperty1<V, VM3>,
    defaultState3: S3,
    vm4: KProperty1<V, VM4>,
    defaultState4: S4,
    vm5: KProperty1<V, VM5>,
    defaultState5: S5,
    vm6: KProperty1<V, VM6>,
    defaultState6: S6,
    val vm7: KProperty1<V, VM7>,
    val defaultState7: S7
) : SixStatesBuilder<V, S1, VM1, S2, VM2, S3, VM3, S4, VM4, S5, VM5, S6, VM6>(
    vm1,
    defaultState1,
    vm2,
    defaultState2,
    vm3,
    defaultState3,
    vm4,
    defaultState4,
    vm5,
    defaultState5,
    vm6,
    defaultState6
) {

    init {
        vm7 setStateTo defaultState7
    }

    /**
     * Define a state to be used when mocking your seventh view model (as defined in the top level mock method).
     * If this method isn't called, your default state will be used automatically.
     * For convenience, the receiver of the lambda is the default state.
     */
    fun viewModel7(stateBuilder: S7.() -> S7) {
        vm7 setStateTo defaultState7.stateBuilder()
    }
}

/**
 * This placeholder can be used as a NO-OP implementation of [MockableMavericksView.provideMocks].
 */
object EmptyMocks : EmptyMavericksViewMocks()

open class EmptyMavericksViewMocks : MavericksViewMocks<MockableMavericksView, Nothing>(allowCreationOfThisInstance = true) {
    override val mocks: List<MavericksMock<MockableMavericksView, out Nothing>> = emptyList()
    override val mockGroups: List<List<MavericksMock<MockableMavericksView, out Nothing>>> = emptyList()
}

/**
 * Defines a set of mocks for a Mavericks view.
 *
 * Use helper functions such as [mockSingleViewModel] to create this, instead of creating it directly.
 */
open class MavericksViewMocks<V : MockableMavericksView, Args : Parcelable> @PublishedApi internal constructor(
    allowCreationOfThisInstance: Boolean = false
) {
    /**
     * A list of mocks to use when testing a view. Each mock represents a unique state to be tested.
     *
     * At least one of [mocks] or [mockGroups] must be implemented.
     */
    open val mocks: List<MavericksMock<V, out Args>> get() = mockGroups.flatten()

    /**
     * An optional breakdown of [mocks] to categorize them into groups.
     *
     * Groups allow splitting up a view with lots of mocks so they can be run in separate tests (better parallelization),
     * with separate default arguments and states. This is useful for complicated views that
     * want to share different default arguments or states with many mocks.
     */
    open val mockGroups: List<List<MavericksMock<V, out Args>>> get() = listOf(mocks)

    init {
        require(allowCreationForTesting || allowCreationOfThisInstance || numAllowedCreationsOfMocks.get() > 0) {
            "Mock creation is not allowed! provideMocks() CANNOT be called directly. " +
                "Instead, call MavericksViewMocks#getFrom()"
        }
    }

    fun validate(viewName: String) {
        // TODO eli_hart: 2018-11-06 Gather all validation errors in one exception instead of failing early, so that you don't have to do multiple test runs to catch multiple issues

        val errorIntro = "Invalid mocks defined for $viewName. "
        val (mocksWithArgsOnly, mocksWithState) = mocks.partition { it.states.isEmpty() }

        fun List<MavericksMock<V, *>>.validateUniqueNames() {
            val nameCounts = groupingBy { it.name }.eachCount()
            nameCounts.forEach { (name, count) ->
                require(count == 1) { "$errorIntro '$name' was used multiple times. Mavericks mock state and argument names must be unique." }
            }
        }

        // We allow args and states to share names, such as "default", since they are different use cases.
        mocksWithArgsOnly.validateUniqueNames()
        mocksWithState.validateUniqueNames()
    }

    interface ViewMocksProvider {
        fun mavericksViewMocks(view: MockableMavericksView): MavericksViewMocks<out MockableMavericksView, out Parcelable>
    }

    object DefaultViewMocksProvider : ViewMocksProvider {
        override fun mavericksViewMocks(view: MockableMavericksView): MavericksViewMocks<out MockableMavericksView, out Parcelable> {
            return provideMocksFunction.call(view) as MavericksViewMocks<out MockableMavericksView, out Parcelable>
        }
    }

    companion object {
        /**
         * Exposed for internal tests to allow us to workaround the requirement that this class
         * can only be created via [getFrom].
         */
        @InternalMavericksApi
        fun <R> allowCreationForTesting(block: () -> R): R {
            allowCreationForTesting = true
            val result: R = block()
            allowCreationForTesting = false
            return result
        }

        private var allowCreationForTesting: Boolean = false

        private val provideMocksFunction: KFunction<*> by lazy {
            MockableMavericksView::class.findFunction("provideMocks")
        }

        /**
         * A Provider of [MavericksViewMocks] will be used to retrieve mocks first before calling [MockableMavericksView.provideMocks].
         */
        var mockProvider: ViewMocksProvider = DefaultViewMocksProvider

        /**
         * Tracks how many mock instances are being validly created. This allows our gating
         * to be done in a thread safe way.
         */
        private val numAllowedCreationsOfMocks = AtomicInteger(0)

        /**
         * Retrieves the mocks from a view provided by [mockProvider].
         * If not set will default to [DefaultViewMocksProvider]
         *
         * All access to mocks is gated behind this function so that it can enforce that
         * mocks are only used in debug mode, and so that this function can access mocks
         * reflectively. By only accessing mocks reflectively they are allowed to be stripped
         * by minification for non debug builds.
         *
         * This returns empty if [MockableMavericks.enableMavericksViewMocking] is disabled.
         */
        fun getFrom(view: MockableMavericksView): MavericksViewMocks<out MockableMavericksView, out Parcelable> {
            if (!MockableMavericks.enableMavericksViewMocking) {
                Log.w("MockBuilder", "Mocks accessed in non debug build")
                return EmptyMocks
            }

            numAllowedCreationsOfMocks.incrementAndGet()

            val mocks = mockProvider.mavericksViewMocks(view)

            require(numAllowedCreationsOfMocks.decrementAndGet() >= 0) {
                "numAllowedCreationsOfMocks is negative"
            }

            return mocks
        }
    }
}

open class MockBuilder<V : MockableMavericksView, Args : Parcelable> internal constructor(
    internal val defaultArgs: Args?,
    vararg defaultStatePairs: Pair<KProperty1<V, MavericksViewModel<MavericksState>>, MavericksState>
) : MavericksViewMocks<V, Args>(), DataClassSetDsl {

    internal val defaultStates = defaultStatePairs.map { MockState(it.first, it.second) }

    @VisibleForTesting
    override val mocks = mutableListOf<MavericksMock<V, Args>>()

    init {
        val viewModelProperties = defaultStates.map { it.viewModelProperty }
        require(viewModelProperties.distinct().size == defaultStates.size) {
            "Duplicate viewmodels were passed to the mock method - ${viewModelProperties.map { it.name }}"
        }

        // Even if args are null this is useful to add because it tests the code flow where initial state
        // is created from its defaults.
        // This isn't necessary in the case of "existingViewModel" tests, but we can't know whether that's the case
        // at this point, so we need to add it anyway.
        addState(
            name = DEFAULT_INITIALIZATION_NAME,
            forInitialization = true,
            type = MavericksMock.Type.DefaultInitialization
        )

        if (defaultStates.isNotEmpty()) {
            addState(
                name = DEFAULT_STATE_NAME,
                type = MavericksMock.Type.DefaultState
            )
            addState(
                name = RESTORED_STATE_NAME,
                statesProvider = { defaultStates.map { it.copy(state = it.state.toRestoredState()) } },
                type = MavericksMock.Type.ProcessRecreation
            )
        }
    }

    /**
     * Provide an instance of arguments to use when initializing a view and creating initial view model state.
     * It is only valid to call this if you defined default arguments in the top level mock method.
     * For convenience, the receiver of the lambda is the default arguments.
     *
     * @param name Describes what state these arguments put the view in. Should be unique.
     */
    fun args(name: String, builder: Args.() -> Args) {
        addState(name, argsProvider = evaluateArgsLambda(builder), forInitialization = true)
    }

    internal fun evaluateArgsLambda(builder: (Args.() -> Args)?): () -> Args? {
        if (builder == null) {
            return { defaultArgs }
        }

        requireNotNull(defaultArgs) { "Args cannot be provided unless you have set a default value for them in the top level mock method" }

        return { builder.invoke(defaultArgs) }
    }

    protected fun addState(
        name: String,
        argsProvider: () -> Args? = { defaultArgs },
        statesProvider: () -> List<MockState<V, *>> = { defaultStates },
        forInitialization: Boolean = false,
        type: MavericksMock.Type = MavericksMock.Type.Custom
    ) {
        mocks.add(
            MavericksMock(
                name = name,
                argsProvider = argsProvider,
                statesProvider = statesProvider,
                forInitialization = forInitialization,
                type = type
            )
        )
    }

    internal fun build(view: V) {
        validate(view::class.java.simpleName)
    }

    private fun <S : MavericksState> S.toRestoredState(): S {
        val klass = this::class

        /** Filter out params that don't have an associated @PersistState prop.
         * Map the parameter name to the current value of its associated property
         * Reduce the @PersistState parameters into a bundle mapping the parameter name to the property value.
         */
        val primaryConstructor = klass.primaryConstructor ?: error("No primary constructor for $this")

        return primaryConstructor
            .parameters
            .filter { kParameter ->
                kParameter.annotations.any { it.annotationClass == PersistState::class } || !kParameter.isOptional
            }
            .map { param ->
                val prop = klass.declaredMemberProperties.single { it.name == param.name }

                @Suppress("UNCHECKED_CAST")
                val value = (prop as? KProperty1<S, Any?>)?.get(this)
                param to value
            }
            .let { pairs ->
                primaryConstructor.callBy(pairs.toMap())
            }
    }
}
