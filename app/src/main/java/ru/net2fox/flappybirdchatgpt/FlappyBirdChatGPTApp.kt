package ru.net2fox.flappybirdchatgpt

import android.app.Application
import com.google.android.material.color.DynamicColors

class FlappyBirdChatGPTApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}