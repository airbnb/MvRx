package com.airbnb.mvrx.helloDagger.di

import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.helloDagger.base.MvRxViewModel

/**
 * Creates and returns instances of the requested ViewModel.
 *
 * This interface serves as a supertype for AssistedInject factories in ViewModels.
 */
interface AssistedViewModelFactory<VM: MvRxViewModel<S>, S: MvRxState> {
    fun create(state: S): VM
}