package com.airbnb.mvrx.hellodagger

import android.app.Application
import androidx.fragment.app.FragmentActivity
import com.airbnb.mvrx.hellodagger.di.AppComponent
import com.airbnb.mvrx.hellodagger.di.DaggerAppComponent

class HelloDaggerApplication : Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.create()
        com.airbnb.mvrx.mocking.MavericksMocks.install(this)
    }
}

fun FragmentActivity.appComponent(): AppComponent {
    return (application as HelloDaggerApplication).appComponent
}
