package com.airbnb.mvrx.launcher

import com.airbnb.mvrx.mock.MockedViewProvider

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.BaseMvRxViewModel
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.ViewModelContext
import com.airbnb.mvrx.launcher.MvRxLauncherActivity.Companion.PARAM_VIEW_PATTERN_TO_TEST
import com.airbnb.mvrx.launcher.MvRxLauncherActivity.Companion.PARAM_VIEW_TO_OPEN
import com.airbnb.mvrx.mock.getMockVariants
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class LauncherState(
    val cachedMocks: Async<List<MockedViewProvider<*>>> = Loading(),
    val allMocks: Async<List<MockedViewProvider<*>>> = Loading(),
    /** The FQN name of the MvRxView that is currently selected for display. */
    val selectedView: String? = null,
    /**
     * The currently selected mock. This is set when the user clicks into a mock.
     * Additionally, the value is saved and restored when mocks are loaded anew when the viewmodel is initialized.
     */
    val selectedMock: MockedViewProvider<*>? = null,
    /** Details about the most recently used MvRxViews and mocks. The UI can use this to order information for relevance. */
    val recentUsage: RecentUsage = RecentUsage(),
    val viewNamePatternToTest: String? = null,
    val viewNameToOpen: String? = null
) : MvRxState

data class RecentUsage(
    /** Ordered list of most recently used MvRxViews, by FQN. */
    val viewNames: List<String> = emptyList(),
    /** Ordered list of most recently used mocks. */
    val mockIdentifiers: List<MockIdentifier> = emptyList()
) {
    /**
     * Move the given view name to the top of the recents list.
     * Returns a new instance with the list modified.
     */
    fun withViewAtTop(viewName: String?): RecentUsage {
        if (viewName == null) return this

        return copy(
            viewNames = listOf(viewName) + viewNames.minus(viewName)
        )
    }
}

data class MockIdentifier(val ViewName: String, val mockName: String) {
    constructor(mockedViewProvider: MockedViewProvider<*>) : this(
        mockedViewProvider.viewName,
        mockedViewProvider.mockData.name
    )
}

class LauncherViewModel(
    private val initialState: LauncherState,
    private val sharedPrefs: SharedPreferences
) : BaseMvRxViewModel<LauncherState>(initialState) {

    init {
        loadViewsFromCache(initialState)

        GlobalScope.launch {
            val mocks = MockedViews.MOCKED_VIEW_PROVIDERS
            setState {
                // The previously selected view (last time the app ran) may have been deleted or renamed.
                val selectedViewExists = mocks.any { it.viewName == selectedView }

                copy(
                    allMocks = Success(mocks),
                    selectedView = if (selectedViewExists) selectedView else null
                )
            }

            saveViewsToCache(mocks)
        }
    }

    /** Since parsing views from dex files is slow we can remember the last list of view names and load them directly. */
    private fun loadViewsFromCache(initialState: LauncherState) = GlobalScope.launch {
        val selectedMockData: String? = sharedPrefs.getString(KEY_SELECTED_MOCK, null)
        val (selectedMocksViewName, selectedMockName) = selectedMockData?.split(
            PROPERTY_SEPARATOR
        ) ?: listOf(null, null)

        fun List<MockedViewProvider<*>>.findSelectedMock(): MockedViewProvider<*>? {
            return find { it.viewName == selectedMocksViewName && it.mockData.name == selectedMockName }
        }

        // The goal with saving and restoring the selected mock is to launch the last used mock automatically, ASAP, with the expectation
        // that the developer will be needing to use it again (ie ongoing development on that screen.
        // A few seconds can be a big deal here, so to be as instant as possible we sort the views to parse the recent one first,
        // process them all in parallel with coroutines (with the recent mock kicked off first), and as soon as it is ready we update
        // state, without waiting for the rest of the views to finish.
        // This changes the loading time from ~5 seconds to nearly instantaneous (depends how complex the mocks are for the recent view).
        sharedPrefs
            .getList(KEY_VIEWS)
            .sortedWith(viewUiOrderComparator(initialState))
            // Mocks are loaded one at a time, in order recency, so we can prioritize loading
            // the view that is most likely to be needed first.
            .onEach { viewName ->
                try {
                    val mocks = getMockVariants(viewName)

                    // Only set the selected mock if a deeplink wasn't used to open view,
                    // because otherwise they interfere with each other.
                    if (this@LauncherViewModel.initialState.viewNameToOpen == null && this@LauncherViewModel.initialState.viewNamePatternToTest == null) {
                        mocks?.findSelectedMock()?.let { selectedMock ->
                            setState { copy(selectedMock = selectedMock) }
                        }
                    }

                    setState {
                        copy(cachedMocks = Success(cachedMocks().orEmpty() + mocks.orEmpty()))
                    }
                } catch (e: ClassNotFoundException) {
                    // The stored view name might not exist anymore if a different flavor was built or a view was deleted
                }
            }
    }

    private fun saveViewsToCache(mockedViewProviders: List<MockedViewProvider<*>>) {
        sharedPrefs.edit {
            // Get fully qualified names and remove duplicates
            putList(KEY_VIEWS, mockedViewProviders.map { it.viewName }.distinct())
        }
    }

    fun setSelectedView(viewName: String?) {
        setState {
            copy(selectedView = viewName, recentUsage = recentUsage.withViewAtTop(viewName))
        }

        sharedPrefs.edit {
            putString(KEY_SELECTED_VIEW, viewName)

            if (viewName != null) {
                val recentViews = sharedPrefs.getList(KEY_RECENTLY_USED_VIEWS)
                val newList = listOf(viewName) + recentViews.minus(viewName)
                putList(KEY_RECENTLY_USED_VIEWS, newList.take(NUM_RECENT_ITEMS_TO_KEEP))
            }
        }
    }

    fun setSelectedMock(mock: MockedViewProvider<*>?) {
        setState {
            copy(selectedMock = mock)
        }

        sharedPrefs.edit {
            val savedMockValue =
                if (mock == null) null else mock.viewName + PROPERTY_SEPARATOR + mock.mockData.name
            putString(KEY_SELECTED_MOCK, savedMockValue)

            if (savedMockValue != null) {
                val recentMocks = sharedPrefs.getList(KEY_RECENTLY_USED_MOCKS)
                val newList = listOf(savedMockValue) + recentMocks.minus(savedMockValue)
                putList(KEY_RECENTLY_USED_MOCKS, newList.take(NUM_RECENT_ITEMS_TO_KEEP))
            }
        }
    }

    companion object : MvRxViewModelFactory<LauncherViewModel, LauncherState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: LauncherState
        ): LauncherViewModel {
            val sharedPrefs = viewModelContext.sharedPrefs()
            return LauncherViewModel(state, sharedPrefs)
        }

        override fun initialState(viewModelContext: ViewModelContext): LauncherState? {
            val sharedPrefs = viewModelContext.sharedPrefs()
            val selectedView: String? = sharedPrefs.getString(KEY_SELECTED_VIEW, null)
            val recentViews = sharedPrefs.getList(KEY_RECENTLY_USED_VIEWS)

            val recentMocks = sharedPrefs.getList(KEY_RECENTLY_USED_MOCKS)
                .map { it.split(PROPERTY_SEPARATOR) }
                .map { (viewName, mockName) ->
                    MockIdentifier(viewName, mockName)
                }

            val params = viewModelContext.activity.intent?.extras
            // If people want to use multiple words they have to separate them with underscores because its part of the link path
            fun parseParam(name: String) = params?.getString(name)?.replace("_", " ")

            return LauncherState(
                selectedView = selectedView,
                recentUsage = RecentUsage(
                    viewNames = recentViews,
                    mockIdentifiers = recentMocks
                ),
                viewNamePatternToTest = parseParam(PARAM_VIEW_PATTERN_TO_TEST),
                viewNameToOpen = parseParam(PARAM_VIEW_TO_OPEN)
            )
        }

        private fun ViewModelContext.sharedPrefs(): SharedPreferences {
            return activity.getSharedPreferences("MvRxLauncherCache", Context.MODE_PRIVATE)
        }

        private const val KEY_VIEWS = "key_view_names"
        private const val KEY_SELECTED_VIEW = "key_selected_view"
        private const val KEY_SELECTED_MOCK = "key_selected_mock"
        private const val KEY_RECENTLY_USED_VIEWS = "key_recently_used_views"
        private const val KEY_RECENTLY_USED_MOCKS = "key_recently_used_mocks"
        private const val PROPERTY_SEPARATOR = "|#*#|"
        private const val NUM_RECENT_ITEMS_TO_KEEP = 3
    }
}

// Trying to pick a value that won't accidentally appear in names or descriptions
private const val LIST_SEPARATOR = "**@%$"

private fun SharedPreferences.Editor.putList(
    key: String,
    list: List<String>
): SharedPreferences.Editor? {
    require(list.none { it.contains(LIST_SEPARATOR) }) { "String contained the separator key: $list" }
    return putString(key, list.joinToString(separator = LIST_SEPARATOR))
}

private fun SharedPreferences.getList(key: String) =
    getString(key, null)?.split(LIST_SEPARATOR) ?: emptyList()
