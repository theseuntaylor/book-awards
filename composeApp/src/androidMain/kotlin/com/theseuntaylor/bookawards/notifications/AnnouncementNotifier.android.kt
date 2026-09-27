package com.theseuntaylor.bookawards.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.theseuntaylor.bookawards.data.ScheduledAnnouncement
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@Composable
actual fun rememberAnnouncementNotifier(): AnnouncementNotifier {
    val context = LocalContext.current.applicationContext
    val notifier = remember(context) { AndroidAnnouncementNotifier(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), notifier::onPermissionResult)
    SideEffect {
        notifier.launchPermissionRequest = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
    return notifier
}

private class AndroidAnnouncementNotifier(private val context: Context) : AnnouncementNotifier {
    var launchPermissionRequest: (() -> Unit)? = null
    private var pendingPermission: CompletableDeferred<Boolean>? = null
    private val workManager get() = WorkManager.getInstance(context)

    fun onPermissionResult(granted: Boolean) {
        pendingPermission?.complete(granted)
        pendingPermission = null
    }

    override suspend fun requestPermission(): Boolean {
        if (hasNotificationPermission(context)) return true
        val launch = launchPermissionRequest ?: return false
        val result = CompletableDeferred<Boolean>().also { pendingPermission = it }
        launch()
        return result.await()
    }

    override fun schedule(announcements: List<ScheduledAnnouncement>) {
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        for (announcement in announcements) {
            val delay = announcement.fireAt.toInstant(timeZone) - now
            val request = OneTimeWorkRequestBuilder<AnnouncementWorker>()
                .setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        AnnouncementWorker.KEY_ID to announcement.id,
                        AnnouncementWorker.KEY_TITLE to announcement.title,
                        AnnouncementWorker.KEY_BODY to announcement.body
                    )
                )
                .build()
            workManager.enqueueUniqueWork(announcement.id, ExistingWorkPolicy.REPLACE, request)
        }
    }

    override fun cancel(ids: List<String>) {
        ids.forEach(workManager::cancelUniqueWork)
    }
}

internal fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
