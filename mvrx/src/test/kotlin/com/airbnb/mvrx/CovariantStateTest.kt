package com.airbnb.mvrx

import android.os.Bundle
import androidx.fragment.app.testing.launchFragment
import org.junit.Test

class CovariantStateTest : BaseTest() {
    class FragmentWithFiveAbstractViewModelDeclarations : FragmentWithAbstractViewModelDeclaration() {
        private val parentViewModel2: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
        )
        private val parentViewModel3: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
        )
        private val parentViewModel4: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
        )
        private val parentViewModel5: ParentViewModel<out ParentState> by fragmentViewModel(
            viewModelClass = ChildViewModel::class,
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            withState(parentViewModel) {}
            withState(parentViewModel, parentViewModel2) { _, _ -> }
            withState(parentViewModel, parentViewModel2, parentViewModel3) { _, _, _ -> }
            withState(
                parentViewModel,
                parentViewModel2,
                parentViewModel3,
                parentViewModel4
            ) { _, _, _, _ -> }
            withState(
                parentViewModel,
                parentViewModel2,
                parentViewModel3,
                parentViewModel4,
                parentViewModel5
            ) { _, _, _, _, _ -> }
        }
    }

    /**
     * Tests that the [withState] calls in [FragmentWithFiveAbstractViewModelDeclarations.onCreate] do not error
     */
    @Test
    fun testWithStateCallsWithCovariantStates() {
        launchFragment<FragmentWithFiveAbstractViewModelDeclarations>()
    }
}