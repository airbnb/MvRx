package com.airbnb.mvrx

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import org.junit.Test

class CovariantMvRxViewModelSubscriberTest : BaseTest() {
    abstract class ParentMvRxViewModel<S : ParentState>(initialState: S) : BaseMvRxViewModel<S>(initialState)

    abstract class ParentState : MvRxState {
        abstract val string: String
        abstract val async: Async<Any>
    }

    data class ChildState(
        override val string: String = "value",
        override val async: Async<Any> = Uninitialized
    ) : ParentState()

    class ChildMvRxViewModel(initialState: ChildState) : ParentMvRxViewModel<ChildState>(initialState)

    class FragmentWithAbstractMvrxViewModelDeclaration : Fragment(), MvRxView {
        private val parentMvRxViewModel: ParentMvRxViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildMvRxViewModel::class,
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            parentMvRxViewModel.selectSubscribe(ParentState::string) {}
            parentMvRxViewModel.selectSubscribe(ParentState::string, ParentState::string) { _, _ -> }
            parentMvRxViewModel.selectSubscribe(ParentState::string, ParentState::string, ParentState::string) { _, _, _ -> }
            parentMvRxViewModel.selectSubscribe(ParentState::string, ParentState::string, ParentState::string, ParentState::string) { _, _, _, _ -> }
            parentMvRxViewModel.selectSubscribe(
                ParentState::string,
                ParentState::string,
                ParentState::string,
                ParentState::string,
                ParentState::string
            ) { _, _, _, _, _ -> }
            parentMvRxViewModel.selectSubscribe(
                ParentState::string,
                ParentState::string,
                ParentState::string,
                ParentState::string,
                ParentState::string,
                ParentState::string
            ) { _, _, _, _, _, _ -> }
            parentMvRxViewModel.selectSubscribe(
                ParentState::string,
                ParentState::string,
                ParentState::string,
                ParentState::string,
                ParentState::string,
                ParentState::string,
                ParentState::string
            ) { _, _, _, _, _, _, _ -> }

            parentMvRxViewModel.asyncSubscribe(ParentState::async) {}
            parentMvRxViewModel.subscribe { }
        }

        override fun invalidate() {}
    }

    /**
     * Tests that the [MvRxView.selectSubscribe] calls in [FragmentWithAbstractMvrxViewModelDeclaration.onCreate] do not error
     */
    @Test
    fun testSubscribeCallsWithCovariantStates() {
        launchFragment<FragmentWithAbstractMvrxViewModelDeclaration>()
    }
}