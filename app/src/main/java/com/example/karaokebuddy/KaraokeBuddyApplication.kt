package com.example.karaokebuddy

import android.app.Application
import timber.log.Timber

class KaraokeBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
