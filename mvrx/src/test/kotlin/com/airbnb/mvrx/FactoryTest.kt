package com.airbnb.mvrx

import android.app.Application
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import kotlinx.android.parcel.Parcelize
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import java.lang.reflect.InvocationTargetException

data class FactoryState(val greeting: String = "") : MvRxState {
    constructor(args: TestArgs) : this("${args.greeting} constructor")
}

@Parcelize
data class TestArgs(val greeting: String) : Parcelable

class ViewModelFactoryTestFragment : Fragment()

/**
 * Tests ViewModel creation when there is no factory.
 */
class NoFactoryTest : BaseTest() {

    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        activity = Robolectric.setupActivity(FragmentActivity::class.java)
    }

    @Test
    fun createFromActivityOwner() {
        class MyViewModel(initialState: FactoryState) : TestMvRxViewModel<FactoryState>(initialState)

        val viewModel = MvRxViewModelProvider.get(MyViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }

    @Test
    fun createFromFragmentOwner() {
        val (_, fragment) = createFragment<ViewModelFactoryTestFragment, TestActivity>()
        class MyViewModel(initialState: FactoryState) : TestMvRxViewModel<FactoryState>(initialState)

        val viewModel = MvRxViewModelProvider.get(MyViewModel::class.java, FactoryState::class.java, FragmentViewModelContext(activity, TestArgs("hello"), fragment))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }

    private data class PrivateState(val count1: Int = 0) : MvRxState

    @Test(expected = InvocationTargetException::class)
    fun failOnPrivateState() {
        class MyViewModel(initialState: PrivateState) : TestMvRxViewModel<PrivateState>(initialState)
        // Create a view model to run state validation checks.
        @Suppress("UNUSED_VARIABLE")
        val viewModel = MvRxViewModelProvider.get(MyViewModel::class.java, PrivateState::class.java, ActivityViewModelContext(activity, null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun failOnDefaultState() {
        class MyViewModel(initialState: FactoryState = FactoryState()) : TestMvRxViewModel<FactoryState>(initialState)
        MvRxViewModelProvider.get(MyViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun failOnWrongSingleParameterType() {
        class ViewModel : BaseMvRxViewModel<FactoryState>(initialState = FactoryState(), debugMode = false)
        MvRxViewModelProvider.get(ViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun failOnMultipleParametersAndNoCompanion() {
        class OptionalParamViewModel(initialState: FactoryState, debugMode: Boolean = false) : BaseMvRxViewModel<FactoryState>(initialState, debugMode)
        MvRxViewModelProvider.get(OptionalParamViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun failOnNoViewModelParameters() {
        class OptionalParamViewModel : BaseMvRxViewModel<FactoryState>(initialState = FactoryState(), debugMode = false)
        MvRxViewModelProvider.get(OptionalParamViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, null))
    }
}

/**
 * Test a factory which only uses a custom ViewModel create.
 */
class FactoryViewModelTest : BaseTest() {

    private class TestFactoryViewModel(initialState: FactoryState, val otherProp: Long) : TestMvRxViewModel<FactoryState>(initialState) {
        companion object : MvRxViewModelFactory<TestFactoryViewModel, FactoryState> {
            override fun create(viewModelContext: ViewModelContext, state: FactoryState): TestFactoryViewModel {
                return when (viewModelContext) {
                    // Use Fragment args to test that there is a valid fragment reference.
                    is FragmentViewModelContext -> TestFactoryViewModel(state, viewModelContext.fragment.arguments?.getLong("otherProp")!!)
                    else -> TestFactoryViewModel(state, 5L)
                }
            }

        }
    }

    private class TestFactoryJvmStaticViewModel(initialState: FactoryState, val otherProp: Long) : TestMvRxViewModel<FactoryState>(initialState) {
        companion object : MvRxViewModelFactory<TestFactoryJvmStaticViewModel, FactoryState> {
            @JvmStatic
            override fun create(viewModelContext: ViewModelContext, state: FactoryState) = TestFactoryJvmStaticViewModel(state, 5)
        }
    }

    private class TestNullFactory(initialState: FactoryState) : TestMvRxViewModel<FactoryState>(initialState) {
        companion object : MvRxViewModelFactory<TestFactoryViewModel, FactoryState> {
            override fun create(viewModelContext: ViewModelContext, state: FactoryState) = null
        }
    }

    private class ViewModelContextApplicationFactory(initialState: FactoryState) : TestMvRxViewModel<FactoryState>(initialState) {
        companion object : MvRxViewModelFactory<TestFactoryJvmStaticViewModel, FactoryState> {
            override fun create(viewModelContext: ViewModelContext, state: FactoryState): TestFactoryJvmStaticViewModel? {
                // If this doesn't crash then there was an application that successfully casted.
                viewModelContext.app<Application>()
                return null
            }
        }
    }

    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        activity = Robolectric.setupActivity(FragmentActivity::class.java)
    }

    @Test
    fun createFromActivityOwner() {
        val viewModel = MvRxViewModelProvider.get(TestFactoryViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }

    @Test
    fun createFromFragmentOwner() {
        val (_, fragment) = createFragment<ViewModelFactoryTestFragment, TestActivity>()
        fragment.arguments = Bundle().apply { putLong("otherProp", 6L) }
        val viewModel = MvRxViewModelProvider.get(TestFactoryViewModel::class.java, FactoryState::class.java, FragmentViewModelContext(activity, TestArgs("hello"), fragment))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
        assertEquals(6, viewModel.otherProp)
    }

    @Test
    fun createWithJvmStatic() {
        val viewModel = MvRxViewModelProvider.get(TestFactoryJvmStaticViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }

    @Test
    fun nullInitialStateDelegatesToConstructor() {
        val viewModel = MvRxViewModelProvider.get(TestNullFactory::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }

    @Test
    fun testApplicationCanBeAccessed() {
        MvRxViewModelProvider.get(ViewModelContextApplicationFactory::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
    }
}

/**
 * Test a factory which only uses a custom initialState.
 */
class FactoryStateTest : BaseTest() {

    private class TestFactoryViewModel(initialState: FactoryState) : TestMvRxViewModel<FactoryState>(initialState) {
        companion object : MvRxViewModelFactory<TestFactoryViewModel, FactoryState> {
            override fun initialState(viewModelContext: ViewModelContext): FactoryState? {
                return when (viewModelContext) {
                    is FragmentViewModelContext -> FactoryState("${viewModelContext.fragment.arguments?.getString("greeting")!!} and ${viewModelContext.args<TestArgs>().greeting} factory")
                    else -> FactoryState("${viewModelContext.args<TestArgs>().greeting} factory")
                }

            }
        }
    }

    private class TestFactoryJvmStaticViewModel(initialState: FactoryState) : TestMvRxViewModel<FactoryState>(initialState) {
        companion object : MvRxViewModelFactory<TestFactoryJvmStaticViewModel, FactoryState> {
            override fun initialState(viewModelContext: ViewModelContext): FactoryState? = FactoryState("${viewModelContext.args<TestArgs>().greeting} factory")
        }
    }

    private class TestNullFactory(initialState: FactoryState) : TestMvRxViewModel<FactoryState>(initialState) {
        companion object : MvRxViewModelFactory<TestNullFactory, FactoryState> {
            override fun initialState(viewModelContext: ViewModelContext): FactoryState? = null
        }
    }

    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        activity = Robolectric.setupActivity(FragmentActivity::class.java)
    }

    @Test
    fun createFromActivityOwner() {
        val viewModel = MvRxViewModelProvider.get(TestFactoryViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
    }

    @Test
    fun createFromFragmentOwner() {
        val (_, fragment) = createFragment<ViewModelFactoryTestFragment, TestActivity>()
        fragment.arguments = Bundle().apply { putString("greeting", "howdy") }
        val viewModel = MvRxViewModelProvider.get(TestFactoryViewModel::class.java, FactoryState::class.java, FragmentViewModelContext(activity, TestArgs("hello"), fragment))
        withState(viewModel) { state ->
            assertEquals(FactoryState("howdy and hello factory"), state)
        }
    }

    @Test
    fun createWithJvmStatic() {
        val viewModel = MvRxViewModelProvider.get(TestFactoryJvmStaticViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
    }

    @Test
    fun nullInitialStateDelegatesToConstructor() {
        val viewModel = MvRxViewModelProvider.get(TestNullFactory::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello constructor"), state)
        }
    }
}

/**
 * Test a factory which uses both a custom State and ViewModel create.
 */
class FactoryViewModelAndStateTest : BaseTest() {

    private class TestFactoryViewModel(initialState: FactoryState, val otherProp: Long) : TestMvRxViewModel<FactoryState>(initialState) {
        companion object : MvRxViewModelFactory<TestFactoryViewModel, FactoryState> {
            override fun create(viewModelContext: ViewModelContext, state: FactoryState) = TestFactoryViewModel(FactoryState("${viewModelContext.args<TestArgs>().greeting} factory"), 5)
        }
    }

    private class TestFactoryJvmStaticViewModel(initialState: FactoryState, val otherProp: Long) : TestMvRxViewModel<FactoryState>(initialState) {
        companion object : MvRxViewModelFactory<TestFactoryJvmStaticViewModel, FactoryState> {
            @JvmStatic
            override fun create(viewModelContext: ViewModelContext, state: FactoryState) = TestFactoryJvmStaticViewModel(FactoryState("${viewModelContext.args<TestArgs>().greeting} factory"), 5)
        }
    }

    private lateinit var activity: FragmentActivity

    @Before
    fun setup() {
        activity = Robolectric.setupActivity(FragmentActivity::class.java)
    }

    @Test
    fun createFromActivityOwner() {
        val viewModel = MvRxViewModelProvider.get(TestFactoryViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }

    @Test
    fun createFromFragmentOwner() {
        val (_, fragment) = createFragment<ViewModelFactoryTestFragment, TestActivity>()

        val viewModel = MvRxViewModelProvider.get(TestFactoryViewModel::class.java, FactoryState::class.java, FragmentViewModelContext(activity, TestArgs("hello"), fragment))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }

    @Test
    fun createWithJvmStatic() {
        val viewModel = MvRxViewModelProvider.get(TestFactoryJvmStaticViewModel::class.java, FactoryState::class.java, ActivityViewModelContext(activity, TestArgs("hello")))
        withState(viewModel) { state ->
            assertEquals(FactoryState("hello factory"), state)
        }
        assertEquals(5, viewModel.otherProp)
    }

}
