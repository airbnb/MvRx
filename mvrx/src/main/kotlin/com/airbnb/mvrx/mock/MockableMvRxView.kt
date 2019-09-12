package com.airbnb.mvrx.mock


import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxView
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.mock.MvRxMock.Companion.DEFAULT_INITIALIZATION_NAME
import com.airbnb.mvrx.mock.MvRxMock.Companion.DEFAULT_STATE_NAME
import com.airbnb.mvrx.mock.MvRxMock.Companion.RESTORED_STATE_NAME
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor


/**
 * A version of [MvRxView] that provides mock State values for the viewmodels used by the view.
 * These mocks enable easier development as well as automated testing.
 *
 * This interface is intended to be used by production classes, such as a base Fragment that other
 * Fragments extend from. Proguard and/or other compile behavior is used to prevent the mock data
 * from being included in production builds.
 */
interface MockableMvRxView : MvRxView {
    fun provideMocks(): MvRxViewMocks<out MockableMvRxView, out Parcelable> = EmptyMocks

    fun enableMockPrinterReceiver() {
        MvRxMockPrinter.startReceiverIfInDebug(this)
    }
}

/**
 * Used with [MockableMvRxView.provideMocks] for mocking a MvRx view that has no view models (eg only static content and arguments).
 *
 * @param defaultArgs If your view takes arguments you must provide an instance of those arguments here to be used as the default value for your mocks.
 *                      If your view has no arguments, pass null (and use Nothing as the type).
 * @param mockBuilder Optionally provide other argument variations via the [MockBuilder] DSL
 */
fun <V : MockableMvRxView, Args : Parcelable> V.mockNoViewModels(
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
inline fun <reified V : MockableMvRxView> V.combineMocks(
    vararg mocks: Pair<String, MvRxViewMocks<V, *>>
): MvRxViewMocks<V, *> = object : MvRxViewMocks<V, Parcelable> {

    init {
        validate(V::class.simpleName!!)
    }

    override val mockGroups: List<List<MvRxMock<V, out Parcelable>>>
        get() {
            return mocks.map { (prefix, mocker) ->
                mocker.mocks.map {
                    it.copy(name = "$prefix : ${it.name}")
                }
            }
        }
}

/**
 * Define state values for a [MockableMvRxView] that should be used in tests.
 * This is for use with [MockableMvRxView.provideMocks] when the view has a single view model.
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
 * @param V The type of MvRx view that is being mocked
 * @param viewModelReference A reference to the view model that will be mocked. Use the "::" operator for this - "MyFragment::myViewModel"
 * @param defaultState An instance of your State that represents the canonical version of your view. It will be the basis for your tests.
 * @param defaultArgs If your view takes arguments you must provide an instance of those arguments here to be used as the default value for your mocks.
 *                      If your view has no arguments, pass null (and use Nothing as the type)
 * @param mockBuilder A lambda where the [SingleViewModelMockBuilder] DSL can be used to specify additional mock variants.
 * @see mockTwoViewModels
 */
fun <V : MockableMvRxView, Args : Parcelable, S : MvRxState> V.mockSingleViewModel(
    viewModelReference: KProperty1<V, BaseMvRxViewModel<S>>,
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
fun <V : MockableMvRxView,
        S1 : MvRxState,
        VM1 : BaseMvRxViewModel<S1>,
        S2 : MvRxState,
        VM2 : BaseMvRxViewModel<S2>,
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
@SuppressWarnings("Detekt.LongParameterList")
fun <V : MockableMvRxView,
        S1 : MvRxState,
        VM1 : BaseMvRxViewModel<S1>,
        S2 : MvRxState,
        VM2 : BaseMvRxViewModel<S2>,
        S3 : MvRxState,
        VM3 : BaseMvRxViewModel<S3>,
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
@SuppressWarnings("Detekt.LongParameterList")
fun <V : MockableMvRxView,
        S1 : MvRxState,
        VM1 : BaseMvRxViewModel<S1>,
        S2 : MvRxState,
        VM2 : BaseMvRxViewModel<S2>,
        S3 : MvRxState,
        VM3 : BaseMvRxViewModel<S3>,
        S4 : MvRxState,
        VM4 : BaseMvRxViewModel<S4>,
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
data class MvRxMock<V : MockableMvRxView, Args : Parcelable> internal constructor(
    val name: String,
    /**
     * Returns the arguments that should be used to initialize the MvRx view. If null, the view models will be
     * initialized purely with the mock states instead.
     */
    val args: Args? = null,
    /**
     * The State to set on each ViewModel in the View. There should be a one to one match.
     */
    val states: List<MockState<V, *>> = emptyList(),
    /**
     * If true, this mock tests the view being created either from arguments or with the viewmodels'
     * default constructor (when [args] is null).
     */
    val forInitialization: Boolean = false,
    val type: Type = Type.Custom
) {

    /**
     * Find a mocked state value for the given view model property.
     *
     * If null is returned it means the initial state should be created through the
     * default mvrx mechanism of arguments.
     */
    fun stateForViewModelProperty(property: KProperty<*>, existingViewModel: Boolean): MvRxState? {
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

    val isDefaultInitialization: Boolean get() = type == Type.DefaultInitialization
    val isDefaultState: Boolean get() = type == Type.DefaultState
    val isForProcessRecreation: Boolean get() = type == Type.ProcessRecreation

    /**
     * There are a set of standard mock variants that all Views have. Beyond that users can define
     * custom mock variants.
     */
    enum class Type {
        /** Uses view arguments to test the initialization pathway of MvRx. */
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
data class MockState<V : MockableMvRxView, S : MvRxState> internal constructor(
    val viewModelProperty: KProperty1<V, BaseMvRxViewModel<S>>,
    val state: S
) {

    /**
     * Forcibly apply this state to the ViewModel instance in the given view.
     * This requires that the view was created with a mock behavior and has a
     * [MockableStateStore] implementation.
     */
    @SuppressLint("VisibleForTests")
    fun applyState(mvrxView: V) {
        viewModelProperty.get(mvrxView).freezeStateForTesting(state)
    }
}

/**
 * Provides a DSL for defining variations to the default mock state.
 */
class SingleViewModelMockBuilder<V : MockableMvRxView, Args : Parcelable, S : MvRxState> internal constructor(
    private val viewModelReference: KProperty1<V, BaseMvRxViewModel<S>>,
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
            args = evaluateArgsLambda(args),
            states = listOf(MockState(viewModelReference, defaultState.stateBuilder()))
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

private fun <V : MockableMvRxView, S : MvRxState, VM : BaseMvRxViewModel<S>> KProperty1<V, VM>.pairDefault(
    state: MvRxState
): Pair<KProperty1<V, BaseMvRxViewModel<MvRxState>>, MvRxState> {
    @Suppress("UNCHECKED_CAST")
    return (this to state) as Pair<KProperty1<V, BaseMvRxViewModel<MvRxState>>, MvRxState>
}

class TwoViewModelMockBuilder<
        V : MockableMvRxView,
        VM1 : BaseMvRxViewModel<S1>,
        S1 : MvRxState,
        VM2 : BaseMvRxViewModel<S2>,
        S2 : MvRxState,
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
            name, evaluateArgsLambda(args), TwoStatesBuilder(
                vm1,
                defaultState1,
                vm2,
                defaultState2
            ).apply(statesBuilder).states
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
        // Split "myProperty" to "My property"
        val asyncName =
            asyncProperty.name.replace(Regex("[A-Z]")) { " ${it.value.toLowerCase()}" }.trim()
                .capitalize()

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
        // Split "myProperty" to "My property"
        val asyncName =
            asyncProperty.name.replace(Regex("[A-Z]")) { " ${it.value.toLowerCase()}" }.trim()
                .capitalize()

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
        V : MockableMvRxView,
        S1 : MvRxState,
        VM1 : BaseMvRxViewModel<S1>,
        S2 : MvRxState,
        VM2 : BaseMvRxViewModel<S2>>
internal constructor(
    val vm1: KProperty1<V, VM1>,
    val defaultState1: S1,
    val vm2: KProperty1<V, VM2>,
    val defaultState2: S2
) {
    private val stateMap = mutableMapOf<KProperty1<V, BaseMvRxViewModel<MvRxState>>, MvRxState>()

    internal val states: List<MockState<V, *>>
        get() = stateMap.map {
            MockState(
                it.key,
                it.value
            )
        }

    protected infix fun <VM : BaseMvRxViewModel<S>, S : MvRxState> KProperty1<V, VM>.setStateTo(
        state: S
    ) {
        @Suppress("UNCHECKED_CAST")
        stateMap[this as KProperty1<V, BaseMvRxViewModel<MvRxState>>] = state
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
        V : MockableMvRxView,
        VM1 : BaseMvRxViewModel<S1>,
        S1 : MvRxState,
        VM2 : BaseMvRxViewModel<S2>,
        S2 : MvRxState,
        VM3 : BaseMvRxViewModel<S3>,
        S3 : MvRxState,
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
            name, evaluateArgsLambda(args), ThreeStatesBuilder(
                vm1,
                defaultState1,
                vm2,
                defaultState2,
                vm3,
                defaultState3
            ).apply(statesBuilder).states
        )
    }
}

open class ThreeStatesBuilder<
        V : MockableMvRxView,
        S1 : MvRxState,
        VM1 : BaseMvRxViewModel<S1>,
        S2 : MvRxState,
        VM2 : BaseMvRxViewModel<S2>,
        S3 : MvRxState,
        VM3 : BaseMvRxViewModel<S3>>
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
        V : MockableMvRxView,
        VM1 : BaseMvRxViewModel<S1>,
        S1 : MvRxState,
        VM2 : BaseMvRxViewModel<S2>,
        S2 : MvRxState,
        VM3 : BaseMvRxViewModel<S3>,
        S3 : MvRxState,
        VM4 : BaseMvRxViewModel<S4>,
        S4 : MvRxState,
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
        )
    }
}

class FourStatesBuilder<
        V : MockableMvRxView,
        S1 : MvRxState,
        VM1 : BaseMvRxViewModel<S1>,
        S2 : MvRxState,
        VM2 : BaseMvRxViewModel<S2>,
        S3 : MvRxState,
        VM3 : BaseMvRxViewModel<S3>,
        S4 : MvRxState,
        VM4 : BaseMvRxViewModel<S4>>
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

/**
 * This placeholder can be used as a NO-OP implementation of [MockableMvRxView.provideMocks].
 */
object EmptyMocks : MvRxViewMocks<MockableMvRxView, Nothing> {
    override val mocks: List<MvRxMock<MockableMvRxView, out Nothing>> = emptyList()
    override val mockGroups: List<List<MvRxMock<MockableMvRxView, out Nothing>>> = emptyList()
}

interface MvRxViewMocks<V : MockableMvRxView, Args : Parcelable> {
    /**
     * A list of mocks to use when testing a view. Each mock represents a unique state to be tested.
     *
     * At least one of [mocks] or [mockGroups] must be implemented.
     */
    val mocks: List<MvRxMock<V, out Args>> get() = mockGroups.flatten()

    /**
     * An optional breakdown of [mocks] to categorize them into groups.
     *
     * Groups allow splitting up a view with lots of mocks so they can be run in separate tests (better parallelization),
     * with separate default arguments and states. This is useful for complicated views that
     * want to share different default arguments or states with many mocks.
     */
    val mockGroups: List<List<MvRxMock<V, out Args>>> get() = listOf(mocks)

    fun validate(viewName: String) {
        // TODO eli_hart: 2018-11-06 Gather all validation errors in one exception instead of failing early, so that you don't have to do multiple test runs to catch multiple issues

        val errorIntro = "Invalid mocks defined for $viewName. "
        val (mocksWithArgsOnly, mocksWithState) = mocks.partition { it.states.isEmpty() }

        fun List<MvRxMock<V, *>>.validateUniqueNames() {
            val nameCounts = groupingBy { it.name }.eachCount()
            nameCounts.forEach { (name, count) ->
                require(count == 1) { "$errorIntro '$name' was used multiple times. MvRx mock state and argument names must be unique." }
            }
        }

        // We allow args and states to share names, such as "default", since they are different use cases.
        mocksWithArgsOnly.validateUniqueNames()
        mocksWithState.validateUniqueNames()
    }
}

open class MockBuilder<V : MockableMvRxView, Args : Parcelable> internal constructor(
    internal val defaultArgs: Args?,
    vararg defaultStatePairs: Pair<KProperty1<V, BaseMvRxViewModel<MvRxState>>, MvRxState>
) : MvRxViewMocks<V, Args>, DataClassSetDsl {

    internal val defaultStates = defaultStatePairs.map { MockState(it.first, it.second) }

    @VisibleForTesting
    override val mocks = mutableListOf<MvRxMock<V, Args>>()

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
            args = defaultArgs,
            states = defaultStates,
            forInitialization = true,
            type = MvRxMock.Type.DefaultInitialization
        )

        if (defaultStates.isNotEmpty()) {
            addState(
                name = DEFAULT_STATE_NAME,
                args = defaultArgs,
                states = defaultStates,
                type = MvRxMock.Type.DefaultState
            )
            addState(
                name = RESTORED_STATE_NAME,
                args = defaultArgs,
                states = defaultStates.map { it.copy(state = it.state.toRestoredState()) },
                type = MvRxMock.Type.ProcessRecreation
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
        addState(name, evaluateArgsLambda(builder), defaultStates, forInitialization = true)
    }

    internal fun evaluateArgsLambda(builder: (Args.() -> Args)?): Args? {
        if (builder == null) {
            return defaultArgs
        }

        requireNotNull(defaultArgs) { "Args cannot be provided unless you have set a default value for them in the top level mock method" }

        return builder.invoke(defaultArgs)
    }

    protected fun addState(
        name: String,
        args: Args? = defaultArgs,
        states: List<MockState<V, *>>,
        forInitialization: Boolean = false,
        type: MvRxMock.Type = MvRxMock.Type.Custom
    ) {
        mocks.add(
            MvRxMock(
                name = name,
                args = args,
                states = states,
                forInitialization = forInitialization,
                type = type
            )
        )
    }

    internal fun build(view: V) {
        validate(view::class.java.simpleName)
    }

    private fun <S : MvRxState> S.toRestoredState(): S {
        val klass = this::class

        /** Filter out params that don't have an associated @PersistState prop.
         * Map the parameter name to the current value of its associated property
         * Reduce the @PersistState parameters into a bundle mapping the parameter name to the property value.
         */
        return klass.primaryConstructor!!
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
                (klass.primaryConstructor as KFunction<S>).callBy(pairs.toMap())
            }
    }
}


