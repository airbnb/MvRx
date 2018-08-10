package com.airbnb.mvrx.sample.features.flow

import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.sample.core.MvRxViewModel

/**
 * [PersistState] will persist the count if Android kills the process in the background
 * and restores it in a new process. To demo this, turn do not keep activities on and switch
 * apps while the flow counter fragment is in the foreground.
 */
data class FlowState(@PersistState val count: Int = 0, val notPersistedCount: Int = 0) : MvRxState

class FlowViewModel(override val initialState: FlowState) : MvRxViewModel<FlowState>() {

    fun setCount(count: Int) = setState { copy(count = count, notPersistedCount = count) }
}