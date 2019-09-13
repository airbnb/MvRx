package com.airbnb.mvrx

import com.airbnb.mvrx.mock.MockBehavior
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ScriptableStateStoreTest : BaseTest() {

    private lateinit var viewModel: TestViewModel

    @Before
    fun setup() {
        MvRx.viewModelConfigProvider.withMockBehavior(MockBehavior(
            initialState = MockBehavior.InitialState.None,
            blockExecutions = MockBehavior.BlockExecutions.No,
            stateStoreBehavior = MockBehavior.StateStoreBehavior.Scriptable
        )) {
            viewModel = TestViewModel()
        }
    }

    @Test
    fun testSetStateCallsIgnored() {
        viewModel.attemptToChangeState(2)
        withState(viewModel) {
            Assert.assertEquals(1, it.foo)
        }
    }

    @Test
    fun testCanScriptState() {
        viewModel.freezeStateForTesting(TestState(foo = 2))
        withState(viewModel) {
            Assert.assertEquals(2, it.foo)
        }
    }

    data class TestState(val foo: Int = 1) : MvRxState

    private class TestViewModel(
        initialState: TestState = TestState()
    ) : BaseMvRxViewModel<TestState>(
        initialState
    ) {

        fun attemptToChangeState(newFoo: Int) {
            setState { copy(foo = newFoo) }
        }
    }

}
