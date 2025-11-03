package com.readle.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.readle.app.MainActivity
import com.readle.app.R
import com.readle.app.data.api.audiobookshelf.AudiobookshelfApiClient
import com.readle.app.data.repository.BookRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AudiobookshelfImportService : Service() {

    @Inject
    lateinit var audiobookshelfApiClient: AudiobookshelfApiClient

    @Inject
    lateinit var bookRepository: BookRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "audiobookshelf_import_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SERVER_URL = "server_url"
        const val EXTRA_TOKEN = "token"

        const val ACTION_PROGRESS = "com.readle.app.IMPORT_PROGRESS"
        const val ACTION_COMPLETE = "com.readle.app.IMPORT_COMPLETE"
        const val ACTION_ERROR = "com.readle.app.IMPORT_ERROR"
        const val EXTRA_CURRENT = "current"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_IMPORTED = "imported"
        const val EXTRA_UPDATED = "updated"
        const val EXTRA_ERROR_MESSAGE = "error_message"

        fun start(context: Context, serverUrl: String, token: String) {
            val intent = Intent(context, AudiobookshelfImportService::class.java).apply {
                putExtra(EXTRA_SERVER_URL, serverUrl)
                putExtra(EXTRA_TOKEN, token)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serverUrl = intent?.getStringExtra(EXTRA_SERVER_URL)
        val token = intent?.getStringExtra(EXTRA_TOKEN)

        if (serverUrl == null || token == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start foreground with initial notification
        startForeground(NOTIFICATION_ID, createNotification(0, 0, "Starting import..."))

        // Start import in background
        serviceScope.launch {
            try {
                audiobookshelfApiClient.initialize(serverUrl)
                val existingBooks = bookRepository.getAllBooks().first()

                val result = audiobookshelfApiClient.importEbooks(
                    token = token,
                    existingBooks = existingBooks,
                    onProgress = { current, total ->
                        // Update notification
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            createNotification(current, total, "Importing books...")
                        )

                        // Broadcast progress
                        sendBroadcast(Intent(ACTION_PROGRESS).apply {
                            setPackage(packageName)
                            putExtra(EXTRA_CURRENT, current)
                            putExtra(EXTRA_TOTAL, total)
                        })
                    }
                )

                result.fold(
                    onSuccess = { importedBooks ->
                        var newBooks = 0
                        var updatedBooks = 0

                        for (book in importedBooks) {
                            if (book.id == 0L) {
                                bookRepository.insertBook(book)
                                newBooks++
                            } else {
                                bookRepository.updateBook(book)
                                updatedBooks++
                            }
                        }

                        // Show completion notification
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            createCompletionNotification(newBooks, updatedBooks, importedBooks.size)
                        )

                        // Broadcast completion
                        sendBroadcast(Intent(ACTION_COMPLETE).apply {
                            setPackage(packageName)
                            putExtra(EXTRA_IMPORTED, newBooks)
                            putExtra(EXTRA_UPDATED, updatedBooks)
                            putExtra(EXTRA_TOTAL, importedBooks.size)
                        })

                        android.util.Log.d("ImportService", "Sent ACTION_COMPLETE broadcast: $newBooks new, $updatedBooks updated")
                        
                        stopSelf()
                    },
                    onFailure = { error ->
                        android.util.Log.e("ImportService", "Import failed: ${error.message}", error)

                        // Show error notification
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            createErrorNotification(error.message ?: "Unknown error")
                        )

                        // Broadcast error
                        sendBroadcast(Intent(ACTION_ERROR).apply {
                            setPackage(packageName)
                            putExtra(EXTRA_ERROR_MESSAGE, error.message ?: "Import failed")
                        })

                        android.util.Log.d("ImportService", "Sent ACTION_ERROR broadcast: ${error.message}")
                        
                        stopSelf()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ImportService", "Import exception: ${e.message}", e)

                // Show error notification
                notificationManager.notify(
                    NOTIFICATION_ID,
                    createErrorNotification(e.message ?: "Unknown error")
                )

                // Broadcast error
                sendBroadcast(Intent(ACTION_ERROR).apply {
                    setPackage(packageName)
                    putExtra(EXTRA_ERROR_MESSAGE, e.message ?: "Import failed")
                })

                android.util.Log.d("ImportService", "Sent ACTION_ERROR broadcast (exception): ${e.message}")
                
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Book Import",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress when importing books from Audiobookshelf"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(current: Int, total: Int, text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_import_title))
            .setSmallIcon(R.drawable.ic_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        if (total > 0) {
            builder.setProgress(total, current, false)
            builder.setContentText(getString(R.string.notification_import_progress_text, current, total))
        } else {
            builder.setProgress(0, 0, true)
            builder.setContentText(text)
        }

        return builder.build()
    }

    private fun createCompletionNotification(imported: Int, updated: Int, total: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_import_complete_title))
            .setContentText(getString(R.string.notification_import_complete_text, imported, updated, total))
            .setSmallIcon(R.drawable.ic_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
    }

    private fun createErrorNotification(message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_import_error_title))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
    }
}


