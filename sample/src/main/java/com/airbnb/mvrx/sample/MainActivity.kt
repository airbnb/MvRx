package com.airbnb.mvrx.sample

import android.content.Intent
import android.os.Bundle
import com.airbnb.mvrx.BaseMvRxActivity
import com.airbnb.mvrx.launcher.MvRxLauncherActivity
import com.airbnb.mvrx.launcher.MvRxLauncherMockActivity

/**
 * Extend this class to get MvRx support out of the box.
 *
 * The purpose of this class is to:
 * 1) Be the host of MvRxFragments. MvRxFragments are the screen unit in MvRx. Activities are meant
 *    to just be the shell for your Fragments. There should be no business logic in your
 *    Activities anymore. Use activityViewModel to share state between screens.
 * 2) Properly configure MvRx so it has things like the correct ViewModelStore.
 *
 * To integrate this into your app. you may:
 * 1) Extend this directly.
 * 2) Replace your BaseActivity super class with this one.
 * 3) Manually integrate this into your base Activity (not recommended).
 */
class MainActivity : BaseMvRxActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // TODO Move to list
        startActivity(Intent(this, MvRxLauncherActivity::class.java))
        finish()
    }

    companion object {
        init {
            // Override the default activity for showing mocks from the launcher
            MvRxLauncherMockActivity.activityToShowMock = LauncherActivity::class
        }
    }
}