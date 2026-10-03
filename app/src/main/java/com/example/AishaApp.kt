package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AishaDatabase
import com.example.data.local.AishaPreferences
import com.example.service.AishaVoiceService

class AishaApp : Application() {

    lateinit var database: AishaDatabase
        private set

    lateinit var preferences: AishaPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        database = AishaDatabase.getDatabase(this)
        preferences = AishaPreferences(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val voiceChannel = NotificationChannel(
                AishaVoiceService.CHANNEL_ID,
                getString(R.string.channel_name_voice),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc_voice)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(voiceChannel)
        }
    }
}
