package com.airbnb.mvrx

/**
 * A Fragment or Activity that will contain MvRxViewModels must implement this to achieve proper state [PersistState]
 * restoration.
 *
 * Call [MvRxViewModelStore.restoreViewModels] from *before onCreate* and
 * [MvRxViewModelStore.saveViewModels] in onSaveInstanceState.
 */
interface MvRxViewModelStoreOwner {
    /**
     * Add this to your Fragment:
     * override val mvrxViewModelStore by lazy { MvRxViewModelStore(viewModelStore) }
     */
    val mvrxViewModelStore: MvRxViewModelStore
}