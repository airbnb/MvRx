package com.airbnb.mvrx.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavBackStackEntry
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.DefaultViewModelDelegateFactory
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksDelegateProvider
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxStateFactory
import com.airbnb.mvrx.MvRxViewModelProvider
import com.airbnb.mvrx.RealMvRxStateFactory
import com.airbnb.mvrx.ViewModelDelegateFactory
import com.airbnb.mvrx._fragmentArgsProvider
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Gets or creates a ViewModel scoped to a navigation graph ID.
 * [IllegalArgumentException] if the navGraphId destination is not on the back stack.
 * [IllegalStateException] if the ViewModel is accessed before onViewCreated and there was a re-configuration or launch after process death
 */
inline fun <reified T, reified VM : MavericksViewModel<S>, reified S : MvRxState> T.navGraphViewModel(
    @IdRes navGraphId: Int,
    viewModelClass: KClass<VM> = VM::class,
    crossinline keyFactory: () -> String = { viewModelClass.java.name }
): MavericksDelegateProvider<T, VM> where T : Fragment, T : MavericksView =
    viewModelNavigationDelegateProvider(
        viewModelClass,
        keyFactory,
        navGraphId = navGraphId
    ) { stateFactory, backStackEntry ->
        MvRxViewModelProvider.get(
            viewModelClass = viewModelClass.java,
            stateClass = S::class.java,
            viewModelContext = FragmentViewModelContext(
                activity = requireActivity(),
                args = _fragmentArgsProvider(),
                fragment = this,
                owner = backStackEntry,
                savedStateRegistry = backStackEntry.savedStateRegistry
            ),
            key = keyFactory(),
            initialStateFactory = stateFactory
        )
    }

/**
 * Creates an object that provides a Lazy ViewModel for use in Fragments.
 */
@PublishedApi
internal inline fun <T, reified VM : MavericksViewModel<S>, reified S : MvRxState> viewModelNavigationDelegateProvider(
    viewModelClass: KClass<VM>,
    crossinline keyFactory: () -> String,
    @IdRes navGraphId: Int,
    noinline viewModelProvider: (stateFactory: MvRxStateFactory<VM, S>, backStackEntry: NavBackStackEntry) -> VM
): MavericksDelegateProvider<T, VM> where T : Fragment, T : MavericksView {
    return object : MavericksDelegateProvider<T, VM>() {

        override operator fun provideDelegate(
            thisRef: T,
            property: KProperty<*>
        ): Lazy<VM> {
            val viewModelDelegateFactory = MvRx.viewModelDelegateFactory
            if (viewModelDelegateFactory !is NavigationViewModelDelegateFactory) {
                throw IllegalStateException(
                    """
                    Navigation ViewModels require that MvRx.viewModelDelegateFactory use an implementation of NavigationViewModelDelegateFactory.
                     
                     To setup the default factory configure MvRx with the default NavigationViewModelDelegateFactory.
                     MvRx.viewModelDelegateFactory = DefaultNavigationViewModelDelegateFactory()
                """.trimIndent()
                )
            }

            return viewModelDelegateFactory.createLazyNavigationViewModel(
                stateClass = S::class,
                fragment = thisRef,
                viewModelProperty = property,
                viewModelClass = viewModelClass,
                keyFactory = { keyFactory() },
                navGraphId = navGraphId,
                viewModelProvider = viewModelProvider
            )
        }
    }
}

interface NavigationViewModelDelegateFactory : ViewModelDelegateFactory {
    fun <S : MvRxState, T, VM : MavericksViewModel<S>> createLazyNavigationViewModel(
        fragment: T,
        viewModelProperty: KProperty<*>,
        viewModelClass: KClass<VM>,
        keyFactory: () -> String,
        stateClass: KClass<S>,
        @IdRes navGraphId: Int,
        viewModelProvider: (stateFactory: MvRxStateFactory<VM, S>, backStackEntry: NavBackStackEntry) -> VM
    ): Lazy<VM> where T : Fragment, T : MavericksView
}

class DefaultNavigationViewModelDelegateFactory(
    private val defaultViewModelDelegateFactory: DefaultViewModelDelegateFactory = DefaultViewModelDelegateFactory()
) : NavigationViewModelDelegateFactory,
    ViewModelDelegateFactory by defaultViewModelDelegateFactory {

    override fun <S : MvRxState, T, VM : MavericksViewModel<S>> createLazyNavigationViewModel(
        fragment: T,
        viewModelProperty: KProperty<*>,
        viewModelClass: KClass<VM>,
        keyFactory: () -> String,
        stateClass: KClass<S>,
        @IdRes navGraphId: Int,
        viewModelProvider: (stateFactory: MvRxStateFactory<VM, S>, backStackEntry: NavBackStackEntry) -> VM
    ): Lazy<VM> where T : Fragment, T : MavericksView {
        return navigationLifecycleAwareLazy(fragment) {
            val backStackEntry = fragment.findNavController().getBackStackEntry(navGraphId)

            viewModelProvider(RealMvRxStateFactory(), backStackEntry)
                .apply { onEachInternal(fragment, action = { fragment.postInvalidate() }) }
        }
    }
}
