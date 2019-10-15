package com.airbnb.mvrx.helloDagger

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.withState
import org.hamcrest.CoreMatchers.not
import org.junit.Test

class HelloFragmentTest {

    @Test
    fun fragmentIsInLoadingStateWhenCreated() {
        val scenario: ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity: MainActivity ->
            val fragment = activity.findFragmentById<HelloFragment>(R.id.rootContainer)

            withState(fragment.viewModel) {
                assert(it.message is Loading)
            }

            onView(withId(R.id.messageTextView)).check(matches(withText(R.string.hello_fragment_loading_text)))
            onView(withId(R.id.helloButton)).check(matches(not(isEnabled())))
        }
    }

}

