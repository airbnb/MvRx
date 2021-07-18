package com.airbnb.mvrx.mocking

import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.fragmentViewModel
import org.junit.Test

class CovariantStateMockTest : BaseTest() {
    abstract class ParentViewModel<S : ParentState>(initialState: S) : MavericksViewModel<S>(initialState)

    abstract class ParentState : MavericksState

    class ChildViewModel(initialState: ChildState) : ParentViewModel<ChildState>(initialState)

    data class ChildState(val string: String = "value") : ParentState()

    class FragmentWithParentViewModelDeclarationAndMocks : Fragment(), MockableMavericksView {
        private val parentViewModel: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )

        override fun invalidate() {}

        override fun provideMocks() = mockSingleViewModel(
            FragmentWithParentViewModelDeclarationAndMocks::parentViewModel,
            ChildState(),
            null
        ) {}
    }

    class FragmentWithTwoParentViewModelDeclarationsAndMocks : Fragment(), MockableMavericksView {
        private val parentViewModel1: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel2: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )

        override fun invalidate() {}

        override fun provideMocks() = mockTwoViewModels(
            FragmentWithTwoParentViewModelDeclarationsAndMocks::parentViewModel1,
            ChildState(),
            FragmentWithTwoParentViewModelDeclarationsAndMocks::parentViewModel2,
            ChildState(),
            null
        ) {}
    }

    class FragmentWithThreeParentViewModelDeclarationsAndMocks : Fragment(), MockableMavericksView {
        private val parentViewModel1: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel2: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel3: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )

        override fun invalidate() {}

        override fun provideMocks() = mockThreeViewModels(
            FragmentWithThreeParentViewModelDeclarationsAndMocks::parentViewModel1,
            ChildState(),
            FragmentWithThreeParentViewModelDeclarationsAndMocks::parentViewModel2,
            ChildState(),
            FragmentWithThreeParentViewModelDeclarationsAndMocks::parentViewModel3,
            ChildState(),
            null,
        ) {}
    }

    class FragmentWithFourParentViewModelDeclarationsAndMocks : Fragment(), MockableMavericksView {
        private val parentViewModel1: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel2: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel3: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel4: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )

        override fun invalidate() {}

        override fun provideMocks() = mockFourViewModels(
            FragmentWithFourParentViewModelDeclarationsAndMocks::parentViewModel1,
            ChildState(),
            FragmentWithFourParentViewModelDeclarationsAndMocks::parentViewModel2,
            ChildState(),
            FragmentWithFourParentViewModelDeclarationsAndMocks::parentViewModel3,
            ChildState(),
            FragmentWithFourParentViewModelDeclarationsAndMocks::parentViewModel4,
            ChildState(),
            null
        ) {}
    }

    class FragmentWithFiveParentViewModelDeclarationsAndMocks : Fragment(), MockableMavericksView {
        private val parentViewModel1: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel2: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel3: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel4: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel5: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )

        override fun invalidate() {}

        override fun provideMocks() = mockFiveViewModels(
            FragmentWithFiveParentViewModelDeclarationsAndMocks::parentViewModel1,
            ChildState(),
            FragmentWithFiveParentViewModelDeclarationsAndMocks::parentViewModel2,
            ChildState(),
            FragmentWithFiveParentViewModelDeclarationsAndMocks::parentViewModel3,
            ChildState(),
            FragmentWithFiveParentViewModelDeclarationsAndMocks::parentViewModel4,
            ChildState(),
            FragmentWithFiveParentViewModelDeclarationsAndMocks::parentViewModel5,
            ChildState(),
            null
        ) {}
    }

    class FragmentWithSixParentViewModelDeclarationsAndMocks : Fragment(), MockableMavericksView {
        private val parentViewModel1: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel2: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel3: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel4: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel5: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel6: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )

        override fun invalidate() {}

        override fun provideMocks() = mockSixViewModels(
            FragmentWithSixParentViewModelDeclarationsAndMocks::parentViewModel1,
            ChildState(),
            FragmentWithSixParentViewModelDeclarationsAndMocks::parentViewModel2,
            ChildState(),
            FragmentWithSixParentViewModelDeclarationsAndMocks::parentViewModel3,
            ChildState(),
            FragmentWithSixParentViewModelDeclarationsAndMocks::parentViewModel4,
            ChildState(),
            FragmentWithSixParentViewModelDeclarationsAndMocks::parentViewModel5,
            ChildState(),
            FragmentWithSixParentViewModelDeclarationsAndMocks::parentViewModel6,
            ChildState(),
            null
        ) {}
    }

    class FragmentWithSevenParentViewModelDeclarationsAndMocks : Fragment(), MockableMavericksView {
        private val parentViewModel1: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel2: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel3: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel4: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel5: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel6: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )
        private val parentViewModel7: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
            stateClass = ChildState::class,
        )

        override fun invalidate() {}

        override fun provideMocks() = mockSevenViewModels(
            FragmentWithSevenParentViewModelDeclarationsAndMocks::parentViewModel1,
            ChildState(),
            FragmentWithSevenParentViewModelDeclarationsAndMocks::parentViewModel2,
            ChildState(),
            FragmentWithSevenParentViewModelDeclarationsAndMocks::parentViewModel3,
            ChildState(),
            FragmentWithSevenParentViewModelDeclarationsAndMocks::parentViewModel4,
            ChildState(),
            FragmentWithSevenParentViewModelDeclarationsAndMocks::parentViewModel5,
            ChildState(),
            FragmentWithSevenParentViewModelDeclarationsAndMocks::parentViewModel6,
            ChildState(),
            FragmentWithSevenParentViewModelDeclarationsAndMocks::parentViewModel7,
            ChildState(),
            null,
        ) {}
    }

    @Test
    fun testRetrievingMocks() {
        MavericksViewMocks.getFrom(FragmentWithParentViewModelDeclarationAndMocks())
        MavericksViewMocks.getFrom(FragmentWithTwoParentViewModelDeclarationsAndMocks())
        MavericksViewMocks.getFrom(FragmentWithThreeParentViewModelDeclarationsAndMocks())
        MavericksViewMocks.getFrom(FragmentWithFourParentViewModelDeclarationsAndMocks())
        MavericksViewMocks.getFrom(FragmentWithFiveParentViewModelDeclarationsAndMocks())
        MavericksViewMocks.getFrom(FragmentWithSixParentViewModelDeclarationsAndMocks())
        MavericksViewMocks.getFrom(FragmentWithSevenParentViewModelDeclarationsAndMocks())
    }
}