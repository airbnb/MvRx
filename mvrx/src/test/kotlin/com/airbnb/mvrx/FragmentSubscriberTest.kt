package com.airbnb.mvrx

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.dropWhile
import org.junit.Assert.assertEquals
import org.junit.Test

data class ViewSubscriberState(val foo: Int = 0) : MavericksState

class ViewSubscriberViewModel(initialState: ViewSubscriberState) : TestMavericksViewModel<ViewSubscriberState>(initialState) {
    fun setFoo(foo: Int) = setState { copy(foo = foo) }
}

@Suppress("DEPRECATION")
open class ViewSubscriberFragment : Fragment(), MavericksView {
    private val viewModel: ViewSubscriberViewModel by fragmentViewModel()

    var subscribeCallCount = 0
    var subscribeUniqueOnlyCallCount = 0
    var subscribeCustomCallCount = 0

    var selectSubscribeValue = -1
    var selectSubscribeCallCount = 0

    var selectSubscribeUniqueOnlyValue = -1
    var selectSubscribeUniqueOnlyCallCount = 0

    var selectSubscribeCustomValue = -1
    var selectSubscribeCustomCallCount = 0

    var invalidateCallCount = 0

    var viewCreatedSubscribeCallCount = 0
    var viewCreatedUniqueOnlyCallCount = 0
    var viewCreatedCustomCallCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onEach { subscribeCallCount++ }
        viewModel.onEach(uniqueOnly("onCreate")) { subscribeUniqueOnlyCallCount++ }
        viewModel.onEach(custom("onCreate")) { subscribeCustomCallCount++ }

        viewModel.onEach(ViewSubscriberState::foo) {
            selectSubscribeValue = it
            selectSubscribeCallCount++
        }
        viewModel.onEach(ViewSubscriberState::foo, uniqueOnly("onCreate")) {
            selectSubscribeUniqueOnlyValue = it
            selectSubscribeUniqueOnlyCallCount++
        }

        viewModel.onEach(ViewSubscriberState::foo, custom("onCreate")) {
            selectSubscribeCustomValue = it
            selectSubscribeCustomCallCount++
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate an arbitrary view so onViewCreated is called.
        return inflater.inflate(R.layout.abc_action_bar_title_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onEach { viewCreatedSubscribeCallCount++ }
        viewModel.onEach(uniqueOnly("onCreateView")) { viewCreatedUniqueOnlyCallCount++ }
        viewModel.onEach(custom("onCreateView")) { viewCreatedCustomCallCount++ }
    }

    fun setFoo(foo: Int) = viewModel.setFoo(foo)

    override fun invalidate() {
        invalidateCallCount++
    }

    private fun custom(customId: String) = custom(customId, true) { flow, deliveryMode ->
        flow.drop(1)
            .dropWhile { it == viewModel.lastDeliveredValue((deliveryMode as? Custom)?.subscriptionId) }
    }
}

class FragmentSubscriberTest : BaseTest() {

    private inline fun <reified T : Fragment> createFragmentInTestActivity() = createFragment<T, TestActivity>(containerId = CONTAINER_ID)

    @Test
    fun testSubscribe() {
        val (_, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.viewCreatedSubscribeCallCount)

        // No change in state does not trigger update.
        fragment.setFoo(0)
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.viewCreatedSubscribeCallCount)

        fragment.setFoo(1)
        assertEquals(2, fragment.subscribeCallCount)
        assertEquals(2, fragment.viewCreatedSubscribeCallCount)
    }

    @Test
    fun testSubscribeUniqueOnly() {
        val (_, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)

        // No change in state does not trigger update.
        fragment.setFoo(0)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)

        fragment.setFoo(1)
        assertEquals(2, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(2, fragment.viewCreatedUniqueOnlyCallCount)
    }

    @Test
    fun testSubscribeCustom() {
        val (_, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(0, fragment.subscribeCustomCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        // No change in state does not trigger update.
        fragment.setFoo(0)
        assertEquals(0, fragment.subscribeCustomCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        fragment.setFoo(1)
        assertEquals(1, fragment.subscribeCustomCallCount)
        assertEquals(1, fragment.viewCreatedCustomCallCount)
    }

    @Test
    fun testSelectSubscribe() {
        val (_, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(0, fragment.selectSubscribeValue)

        // No change in state does not trigger update.
        fragment.setFoo(0)
        assertEquals(0, fragment.selectSubscribeValue)

        fragment.setFoo(1)
        assertEquals(1, fragment.selectSubscribeValue)
    }

    @Test
    fun testSelectSubscribeUniqueOnly() {
        val (_, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(0, fragment.selectSubscribeUniqueOnlyValue)

        // No change in state does not trigger update.
        fragment.setFoo(0)
        assertEquals(0, fragment.selectSubscribeUniqueOnlyValue)

        fragment.setFoo(1)
        assertEquals(1, fragment.selectSubscribeUniqueOnlyValue)
    }

    @Test
    fun testSelectSubscribeCustom() {
        val (_, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(-1, fragment.selectSubscribeCustomValue)

        // No change in state does not trigger update.
        fragment.setFoo(0)
        assertEquals(-1, fragment.selectSubscribeCustomValue)

        fragment.setFoo(1)
        assertEquals(1, fragment.selectSubscribeCustomValue)
        assertEquals(1, fragment.selectSubscribeCustomCallCount)
    }

    @Test
    fun invalidateCalledFromCreateToStart() {
        val (_, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.invalidateCallCount)
    }

    @Test
    fun selectSubscribeFromStopToStartWhenStateChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(0, fragment.selectSubscribeValue)
        assertEquals(0, fragment.selectSubscribeUniqueOnlyValue)

        controller.pause()
        controller.stop()

        fragment.setFoo(1)
        assertEquals(0, fragment.selectSubscribeValue)
        assertEquals(0, fragment.selectSubscribeUniqueOnlyValue)

        controller.start()
        controller.resume()

        assertEquals(1, fragment.selectSubscribeValue)
        assertEquals(1, fragment.selectSubscribeUniqueOnlyValue)
    }

    @Test
    fun selectSubscribeFromStopToStartWhenStateNotChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(0, fragment.selectSubscribeValue)
        assertEquals(1, fragment.selectSubscribeCallCount)

        assertEquals(0, fragment.selectSubscribeUniqueOnlyValue)
        assertEquals(1, fragment.selectSubscribeUniqueOnlyCallCount)

        assertEquals(-1, fragment.selectSubscribeCustomValue)
        assertEquals(0, fragment.selectSubscribeCustomCallCount)

        controller.pause()
        controller.stop()

        assertEquals(0, fragment.selectSubscribeValue)
        assertEquals(1, fragment.selectSubscribeCallCount)

        assertEquals(0, fragment.selectSubscribeUniqueOnlyValue)
        assertEquals(1, fragment.selectSubscribeUniqueOnlyCallCount)

        assertEquals(-1, fragment.selectSubscribeCustomValue)
        assertEquals(0, fragment.selectSubscribeCustomCallCount)

        controller.start()
        controller.resume()

        // If unique is not set to true, we always receive an update when unlocked.
        assertEquals(0, fragment.selectSubscribeValue)
        assertEquals(2, fragment.selectSubscribeCallCount)

        assertEquals(0, fragment.selectSubscribeUniqueOnlyValue)
        assertEquals(1, fragment.selectSubscribeUniqueOnlyCallCount)

        assertEquals(-1, fragment.selectSubscribeCustomValue)
        assertEquals(0, fragment.selectSubscribeCustomCallCount)
    }

    @Test
    fun subscribeFromStopToStartWhenStateChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.viewCreatedSubscribeCallCount)

        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)

        assertEquals(0, fragment.subscribeCustomCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        controller.pause()
        controller.stop()

        fragment.setFoo(1)
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.viewCreatedSubscribeCallCount)

        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)

        assertEquals(0, fragment.subscribeCustomCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        controller.start()
        controller.resume()

        assertEquals(2, fragment.subscribeCallCount)
        assertEquals(2, fragment.viewCreatedSubscribeCallCount)

        assertEquals(2, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(2, fragment.viewCreatedUniqueOnlyCallCount)

        assertEquals(1, fragment.subscribeCustomCallCount)
        assertEquals(1, fragment.viewCreatedCustomCallCount)
    }

    @Test
    fun subscribeFromStopToStartWhenStateNotChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)

        assertEquals(1, fragment.viewCreatedSubscribeCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        controller.pause()
        controller.stop()

        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)

        assertEquals(1, fragment.viewCreatedSubscribeCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        controller.start()
        controller.resume()

        assertEquals(2, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)
    }

    @Test
    fun subscribeOnBackStackResumeWhenStateNotChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)

        assertEquals(1, fragment.viewCreatedSubscribeCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        val fragmentManager = controller.get().supportFragmentManager
        fragmentManager.beginTransaction().replace(CONTAINER_ID, Fragment(), "TAG").addToBackStack(null).commit()

        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)

        assertEquals(1, fragment.viewCreatedSubscribeCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        fragmentManager.popBackStackImmediate()

        assertEquals(2, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)

        assertEquals(2, fragment.viewCreatedSubscribeCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)
    }

    @Test
    fun subscribeOnBackStackResumeWhenStateChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)

        assertEquals(1, fragment.viewCreatedSubscribeCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        val fragmentManager = controller.get().supportFragmentManager
        fragmentManager.beginTransaction().replace(CONTAINER_ID, Fragment(), "TAG").addToBackStack(null).commit()

        // State updates should be paused
        fragment.setFoo(1)
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)

        assertEquals(1, fragment.viewCreatedSubscribeCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        fragmentManager.popBackStackImmediate()

        assertEquals(2, fragment.subscribeCallCount)
        assertEquals(2, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(1, fragment.subscribeCustomCallCount)

        assertEquals(2, fragment.viewCreatedSubscribeCallCount)
        assertEquals(2, fragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)
    }

    /**
     * Robolectric always calls paused during it's configuration change. So we have no good place to dispatch
     * a start change while locked, as calling onPause will unlock the subscriptions. We can achieve this
     * by changing the state in onStop.
     */
    class FragmentWithStateChangeDuringOrientationChange : ViewSubscriberFragment() {

        override fun onStop() {
            super.onStop()
            val subscribeCallCountBeforeFooChange = subscribeCallCount
            val subscribeUniqueOnlyCallCountBeforeFooChange = subscribeUniqueOnlyCallCount
            val subscribeCustomCallCountBeforeFooChange = subscribeCustomCallCount
            this.setFoo(1)
            assertEquals(subscribeCallCountBeforeFooChange, subscribeCallCount)
            assertEquals(subscribeUniqueOnlyCallCountBeforeFooChange, subscribeUniqueOnlyCallCount)
            assertEquals(subscribeCustomCallCountBeforeFooChange, subscribeCustomCallCount)
        }
    }

    @Test
    fun selectSubscribeOnConfigurationChangeWhenStateChanged() {
        val (controller, fragment) = createFragmentInTestActivity<FragmentWithStateChangeDuringOrientationChange>()
        assertEquals(0, fragment.selectSubscribeValue)
        assertEquals(1, fragment.selectSubscribeCallCount)

        assertEquals(0, fragment.selectSubscribeUniqueOnlyValue)
        assertEquals(1, fragment.selectSubscribeUniqueOnlyCallCount)

        assertEquals(-1, fragment.selectSubscribeCustomValue)
        assertEquals(0, fragment.selectSubscribeCustomCallCount)

        // This will set foo to 1. See FragmentWithStateChangeDuringOrientationChange.
        controller.configurationChange(Configuration().apply {
            setToDefaults()
            this.orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        val recreatedFragment = controller.mvRxFragment<FragmentWithStateChangeDuringOrientationChange>()

        // If unique is not set to true, we always receive an update when unlocked.
        assertEquals(1, recreatedFragment.selectSubscribeValue)
        assertEquals(1, recreatedFragment.selectSubscribeCallCount)

        // Even if unique is true, if the state changes we expect a value.
        assertEquals(1, recreatedFragment.selectSubscribeUniqueOnlyValue)
        assertEquals(1, recreatedFragment.selectSubscribeUniqueOnlyCallCount)

        assertEquals(-1, recreatedFragment.selectSubscribeCustomValue)
        assertEquals(0, recreatedFragment.selectSubscribeCustomCallCount)
    }

    @Test
    fun selectSubscribeOnConfigurationChangeWhenStateNotChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(0, fragment.selectSubscribeValue)
        assertEquals(1, fragment.selectSubscribeCallCount)

        assertEquals(0, fragment.selectSubscribeUniqueOnlyValue)
        assertEquals(1, fragment.selectSubscribeUniqueOnlyCallCount)

        assertEquals(-1, fragment.selectSubscribeCustomValue)
        assertEquals(0, fragment.selectSubscribeCustomCallCount)

        controller.configurationChange(Configuration().apply {
            setToDefaults()
            this.orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        val recreatedFragment = controller.mvRxFragment<ViewSubscriberFragment>()

        // If unique is not set to true, we always receive an update when unlocked.
        assertEquals(0, recreatedFragment.selectSubscribeValue)
        assertEquals(1, recreatedFragment.selectSubscribeCallCount)

        assertEquals(-1, recreatedFragment.selectSubscribeUniqueOnlyValue)
        assertEquals(0, recreatedFragment.selectSubscribeUniqueOnlyCallCount)

        assertEquals(-1, recreatedFragment.selectSubscribeCustomValue)
        assertEquals(0, recreatedFragment.selectSubscribeCustomCallCount)
    }

    @Test
    fun subscribeOnConfigurationChangeWhenStateChanged() {
        val (controller, fragment) = createFragmentInTestActivity<FragmentWithStateChangeDuringOrientationChange>()
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)

        assertEquals(1, fragment.viewCreatedSubscribeCallCount)
        assertEquals(1, fragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        // This will set foo to 1. See FragmentWithStateChangeDuringOrientationChange.
        controller.configurationChange(Configuration().apply {
            setToDefaults()
            this.orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        val recreatedFragment = controller.mvRxFragment<FragmentWithStateChangeDuringOrientationChange>()

        // As the value changed, the unique only subscription will be called.
        assertEquals(1, recreatedFragment.subscribeCallCount)
        assertEquals(1, recreatedFragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, recreatedFragment.subscribeCustomCallCount)

        assertEquals(1, recreatedFragment.viewCreatedSubscribeCallCount)
        assertEquals(1, recreatedFragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, recreatedFragment.viewCreatedCustomCallCount)
    }

    @Test
    fun subscribeOnConfigurationChangeWhenStateNotChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.subscribeCallCount)
        assertEquals(1, fragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, fragment.subscribeCustomCallCount)

        assertEquals(1, fragment.viewCreatedSubscribeCallCount)
        assertEquals(1, fragment.viewCreatedSubscribeCallCount)
        assertEquals(0, fragment.viewCreatedCustomCallCount)

        controller.configurationChange(Configuration().apply {
            setToDefaults()
            this.orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        val recreatedFragment = controller.mvRxFragment<ViewSubscriberFragment>()

        // As the value has not changed, the unique only subscription will not be called.
        assertEquals(1, recreatedFragment.subscribeCallCount)
        assertEquals(0, recreatedFragment.subscribeUniqueOnlyCallCount)
        assertEquals(0, recreatedFragment.subscribeCustomCallCount)

        assertEquals(1, recreatedFragment.viewCreatedSubscribeCallCount)
        assertEquals(0, recreatedFragment.viewCreatedUniqueOnlyCallCount)
        assertEquals(0, recreatedFragment.viewCreatedCustomCallCount)
    }

    @Test
    fun invalidateCalledFromStopToStartWhenStateChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.invalidateCallCount)

        controller.pause()
        controller.stop()

        fragment.setFoo(1)
        assertEquals(1, fragment.invalidateCallCount)

        controller.start()
        controller.resume()

        assertEquals(2, fragment.invalidateCallCount)
    }

    @Test
    fun invalidateCalledFromStopToStartWhenStateNotChanged() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        assertEquals(1, fragment.invalidateCallCount)

        controller.pause()
        controller.stop()

        assertEquals(1, fragment.invalidateCallCount)

        controller.start()
        controller.resume()

        assertEquals(2, fragment.invalidateCallCount)
    }

    @Suppress("DEPRECATION")
    class DuplicateUniqueSubscriberFragment : Fragment(), MavericksView {
        private val viewModel: ViewSubscriberViewModel by fragmentViewModel()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            viewModel.onEach(uniqueOnly()) { }
            viewModel.onEach(uniqueOnly()) { }
        }

        override fun invalidate() {}
    }

    @Test(expected = RuntimeException::class)
    fun duplicateUniqueOnlySubscribeCrashes() {
        createFragment<DuplicateUniqueSubscriberFragment, TestActivity>(containerId = CONTAINER_ID)
    }

    @Suppress("DEPRECATION")
    class ParentFragment : Fragment(), MavericksView {

        val viewModel: ViewSubscriberViewModel by fragmentViewModel()

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FrameLayout(requireContext())

        override fun invalidate() {
        }
    }

    @Suppress("DEPRECATION")
    class ChildFragmentWithParentViewModel : Fragment(), MavericksView {

        val viewModel: ViewSubscriberViewModel by parentFragmentViewModel()

        override fun invalidate() {
        }
    }

    @Test
    fun testParentFragment() {
        val (_, parentFragment) = createFragment<ParentFragment, TestActivity>(containerId = CONTAINER_ID)
        val childFragment = ChildFragmentWithParentViewModel()
        parentFragment.childFragmentManager.beginTransaction().add(childFragment, "child").commit()
        assertEquals(parentFragment.viewModel, childFragment.viewModel)
    }

    @Suppress("DEPRECATION")
    class ParentFragmentWithoutViewModel : Fragment(), MavericksView {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FrameLayout(requireContext())

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            childFragmentManager.beginTransaction()
                .add(ChildFragmentWithParentViewModel(), "child1")
                .commit()
            childFragmentManager.beginTransaction()
                .add(ChildFragmentWithParentViewModel(), "child2")
                .commit()
        }

        override fun invalidate() {
        }
    }

    @Test
    fun testChildFragmentsCanShareViewModelWithoutParent() {
        val (_, parentFragment) = createFragment<ParentFragmentWithoutViewModel, TestActivity>(containerId = CONTAINER_ID)
        val childFragment1 = parentFragment.childFragmentManager.findFragmentByTag("child1") as ChildFragmentWithParentViewModel
        val childFragment2 = parentFragment.childFragmentManager.findFragmentByTag("child2") as ChildFragmentWithParentViewModel
        assertEquals(childFragment1.viewModel, childFragment2.viewModel)
    }

    @Suppress("DEPRECATION")
    class EmptyMvRxFragment : Fragment(), MavericksView {
        override fun invalidate() {
        }
    }

    @Test
    fun testCreatesViewModelInTopMostFragment() {
        val (_, parentFragment) = createFragment<ParentFragmentWithoutViewModel, TestActivity>(containerId = CONTAINER_ID)
        val middleFragment = Fragment()
        parentFragment.childFragmentManager.beginTransaction().add(middleFragment, "middle").commitNow()
        val childFragment1 = ChildFragmentWithParentViewModel()
        middleFragment.childFragmentManager.beginTransaction().add(childFragment1, "child1").commitNow()

        val childFragment2 = ChildFragmentWithParentViewModel()
        parentFragment.childFragmentManager.beginTransaction().add(childFragment2, "child2").commitNow()

        assertEquals(childFragment1.viewModel, childFragment2.viewModel)
    }

    @Suppress("DEPRECATION")
    class FragmentWithTarget : Fragment(), MavericksView {
        val viewModel: ViewSubscriberViewModel by targetFragmentViewModel()

        var invalidateCount = 0

        override fun invalidate() {
            invalidateCount++
        }
    }

    @Test
    fun testTargetFragment() {
        val (_, parentFragment) = createFragment<EmptyMvRxFragment, TestActivity>(containerId = CONTAINER_ID)
        val targetFragment = EmptyMvRxFragment()
        parentFragment.childFragmentManager.beginTransaction().add(targetFragment, "target").commitNow()
        val fragmentWithTarget = FragmentWithTarget()
        @Suppress("DEPRECATION")
        fragmentWithTarget.setTargetFragment(targetFragment, 123)
        parentFragment.childFragmentManager.beginTransaction().add(fragmentWithTarget, "fragment-with-target").commitNow()
        // Make sure subscribe works.
        assertEquals(1, fragmentWithTarget.invalidateCount)
        fragmentWithTarget.viewModel.setFoo(1)
        assertEquals(2, fragmentWithTarget.invalidateCount)
    }

    @Test
    fun testTargetFragmentsShareViewModel() {
        val (_, parentFragment) = createFragment<EmptyMvRxFragment, TestActivity>(containerId = CONTAINER_ID)
        val targetFragment = EmptyMvRxFragment()
        parentFragment.childFragmentManager.beginTransaction().add(targetFragment, "target").commitNow()
        val fragmentWithTarget1 = FragmentWithTarget()
        @Suppress("DEPRECATION")
        fragmentWithTarget1.setTargetFragment(targetFragment, 123)
        parentFragment.childFragmentManager.beginTransaction().add(fragmentWithTarget1, "fragment-with-target1").commitNow()
        val fragmentWithTarget2 = FragmentWithTarget()
        @Suppress("DEPRECATION")
        fragmentWithTarget2.setTargetFragment(targetFragment, 123)
        parentFragment.childFragmentManager.beginTransaction().add(fragmentWithTarget2, "fragment-with-target2").commitNow()
        assertEquals(fragmentWithTarget1.viewModel, fragmentWithTarget2.viewModel)
    }

    /**
     * This would be [IllegalStateException] except it fails during the Fragment transaction so it's a RuntimeException.
     */
    @Test(expected = RuntimeException::class)
    fun testTargetFragmentWithoutTargetCrashes() {
        val (_, parentFragment) = createFragment<EmptyMvRxFragment, TestActivity>(containerId = CONTAINER_ID)
        val fragmentWithTarget = FragmentWithTarget()
        parentFragment.childFragmentManager.beginTransaction().add(fragmentWithTarget, "fragment-with-target").commitNow()
    }

    @Test
    fun testUniqueOnly() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        fragment.setFoo(1)
        assertEquals(2, fragment.selectSubscribeUniqueOnlyCallCount)

        controller.pause()
        controller.stop()
        fragment.setFoo(2)
        fragment.setFoo(1)
        controller.start()
        controller.resume()

        // In MvRx 1.0, this would have been 3. If the value for a uniqueOnly() subscription changed
        // and changed back while stopped, it would redeliver the value even though it was the same
        // as what it received previously.
        assertEquals(2, fragment.selectSubscribeUniqueOnlyCallCount)
    }

    @Test
    fun testCustom() {
        val (controller, fragment) = createFragmentInTestActivity<ViewSubscriberFragment>()
        fragment.setFoo(1)
        assertEquals(1, fragment.selectSubscribeCustomCallCount
        )

        controller.pause()
        controller.stop()
        fragment.setFoo(2)
        fragment.setFoo(1)
        controller.start()
        controller.resume()

        // In MvRx 1.0, this would have been 2. If the value for a uniqueOnly() subscription changed
        // and changed back while stopped, it would redeliver the value even though it was the same
        // as what it received previously.
        assertEquals(1, fragment.selectSubscribeCustomCallCount)
    }
}
