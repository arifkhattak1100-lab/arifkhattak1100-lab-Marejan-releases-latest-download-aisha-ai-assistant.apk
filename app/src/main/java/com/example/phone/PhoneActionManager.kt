package com.example.phone

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import java.util.Calendar

data class ActionResult(
    val success: Boolean,
    val feedbackMessage: String
)

class PhoneActionManager(private val context: Context) {
    companion object {
        private const val TAG = "PhoneActionManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun executeCommand(command: String, arg: String? = null): ActionResult {
        Log.d(TAG, "Executing command: $command with arg: $arg")
        return when (command.uppercase().trim()) {
            "OPEN_WHATSAPP" -> openWhatsApp()
            "OPEN_YOUTUBE" -> openYouTube()
            "OPEN_CAMERA" -> openCamera()
            "OPEN_SETTINGS" -> openSettings()
            "CALL_MOM", "CALL" -> makePhoneCall(arg ?: "Mom")
            "SEND_MESSAGE" -> sendMessage(arg)
            "SET_ALARM" -> setAlarm(arg)
            "PLAY_MUSIC" -> toggleMediaPlayback(play = true)
            "PAUSE_MUSIC" -> toggleMediaPlayback(play = false)
            "INCREASE_VOLUME" -> adjustVolume(increase = true)
            "DECREASE_VOLUME" -> adjustVolume(increase = false)
            "TURN_ON_BLUETOOTH", "OPEN_BLUETOOTH" -> openBluetoothSettings()
            "SHOW_NOTIFICATIONS" -> openNotifications()
            else -> ActionResult(false, "Unknown command: $command")
        }
    }

    private fun openWhatsApp(): ActionResult {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.whatsapp")
                ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(true, "Opened WhatsApp")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open WhatsApp: ${e.message}")
            ActionResult(false, "Could not open WhatsApp: ${e.message}")
        }
    }

    private fun openYouTube(): ActionResult {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.google.android.youtube")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(true, "Opened YouTube")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open YouTube: ${e.message}")
            ActionResult(false, "Could not open YouTube: ${e.message}")
        }
    }

    private fun openCamera(): ActionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult(true, "Opened Camera")
            } else {
                val fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ActionResult(true, "Opened Camera")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Camera: ${e.message}")
            ActionResult(false, "Could not open Camera: ${e.message}")
        }
    }

    private fun openSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "Opened Settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Settings: ${e.message}")
            ActionResult(false, "Could not open Settings: ${e.message}")
        }
    }

    private fun makePhoneCall(recipient: String): ActionResult {
        return try {
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCallPermission) {
                // If specific number passed, use ACTION_CALL, else fallback to dialer
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Opened phone dialer for $recipient")
            } else {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Opened phone dialer to call $recipient")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call: ${e.message}")
            ActionResult(false, "Could not place call: ${e.message}")
        }
    }

    private fun sendMessage(details: String?): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                if (!details.isNullOrBlank()) {
                    putExtra("sms_body", details)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "Opened messages")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open messaging: ${e.message}")
            ActionResult(false, "Could not open messaging: ${e.message}")
        }
    }

    private fun setAlarm(timeArg: String?): ActionResult {
        return try {
            val calendar = Calendar.getInstance()
            // Set 1 hour ahead by default if no time specified
            calendar.add(Calendar.HOUR_OF_DAY, 1)

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, timeArg ?: "Alarm set by Aisha")
                putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
                putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE))
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "Alarm set in Clock app")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm: ${e.message}")
            ActionResult(false, "Could not set alarm: ${e.message}")
        }
    }

    private fun toggleMediaPlayback(play: Boolean): ActionResult {
        return try {
            val keyCode = if (play) KeyEvent.KEYCODE_MEDIA_PLAY else KeyEvent.KEYCODE_MEDIA_PAUSE
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            audioManager.dispatchMediaKeyEvent(downEvent)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(upEvent)
            ActionResult(true, if (play) "Resumed music playback" else "Paused music playback")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle music: ${e.message}")
            ActionResult(false, "Could not change music state: ${e.message}")
        }
    }

    private fun adjustVolume(increase: Boolean): ActionResult {
        return try {
            val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            ActionResult(true, if (increase) "Increased volume" else "Decreased volume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust volume: ${e.message}")
            ActionResult(false, "Could not adjust volume: ${e.message}")
        }
    }

    private fun openBluetoothSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "Opened Bluetooth settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Bluetooth settings: ${e.message}")
            ActionResult(false, "Could not open Bluetooth settings: ${e.message}")
        }
    }

    private fun openNotifications(): ActionResult {
        return try {
            // Try standard notification settings
            val intent = Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "Opened Notification settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notifications: ${e.message}")
            ActionResult(false, "Could not access notifications: ${e.message}")
        }
    }
}
