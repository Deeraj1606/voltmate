package com.company.voltmate

import android.app.Application
import android.content.Context
import com.company.voltmate.di.AppContainer
import com.company.voltmate.di.DefaultAppContainer
import org.osmdroid.config.Configuration

class VoltMateApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // osmdroid configuration
        try {
            val prefs = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
            Configuration.getInstance().load(this, prefs)
            Configuration.getInstance().userAgentValue = packageName
        } catch (e: Exception) {
            Configuration.getInstance().userAgentValue = packageName
        }
    }
}
