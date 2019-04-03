package com.airbnb.mvrx.dogs.app

import android.app.Application
import com.airbnb.mvrx.dogs.data.DogRepository

/**
 * Launcher icon made by Freepik at flaticon.com
 */
class DogApplication : Application() {
    val dogsRespository = DogRepository()
}