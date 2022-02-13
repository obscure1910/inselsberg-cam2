package de.obscure.webcam

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy

import timber.log.Timber

class WebcamApplication : Application() {

    override fun onCreate() {
//        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            StrictMode.setVmPolicy(
//                VmPolicy.Builder()
//                    .detectNonSdkApiUsage()
//                    .penaltyLog()
//                    .build()
//            )
//        }

        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }

}