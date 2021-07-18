package com.airbnb.mvrx

import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import kotlinx.parcelize.Parcelize
import org.junit.Test
import kotlin.reflect.KClass

abstract class ParentViewModel<S : ParentState>(initialState: S) : MavericksViewModel<S>(initialState)

abstract class ParentState : MavericksState

class ChildViewModel(initialState: ChildState) : ParentViewModel<ChildState>(initialState)

data class ChildState(val string: String = "value") : ParentState()

@Parcelize
class FragmentWithAbstractViewModelDeclarationArgs(
    val viewModelClass: Class<out ParentViewModel<out ParentState>>,
    val stateClass: Class<out ParentState>
) : Parcelable {
    constructor(viewModelClass: KClass<out ParentViewModel<out ParentState>>, stateClass: KClass<out ParentState>) : this(
        viewModelClass.java,
        stateClass.java
    )
}

open class FragmentWithAbstractViewModelDeclaration : Fragment(), MavericksView {
    private val args: FragmentWithAbstractViewModelDeclarationArgs by args()

    val parentViewModel: ParentViewModel<out ParentState> by fragmentViewModel(
        viewModelClass = args.viewModelClass.kotlin,
        stateClass = args.stateClass.kotlin
    )

    override fun invalidate() {
    }
}

class AbstractViewModelDeclarationTest : BaseTest() {
    /**
     * Tests that [FragmentWithAbstractViewModelDeclaration] can create an instance of [ChildViewModel]
     * using [KClass] arguments to the delegates in MavericksExtensions.kt, while declaring it as
     * an abstract parent class.
     */
    @Test
    fun testFragmentIsCreatedWithCorrectViewModelType() {
        launchFragment<FragmentWithAbstractViewModelDeclaration>(
            bundleOf(
                Mavericks.KEY_ARG to FragmentWithAbstractViewModelDeclarationArgs(
                    ChildViewModel::class, ChildState::class
                )
            )
        ).onFragment { assert(it.parentViewModel is ChildViewModel) }
    }
}