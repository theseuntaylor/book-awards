package com.theseuntaylor.bookawards.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.theseuntaylor.bookawards.data.ScheduledAnnouncement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.datetime.number
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun rememberAnnouncementNotifier(): AnnouncementNotifier = remember { IosAnnouncementNotifier() }

private class IosAnnouncementNotifier : AnnouncementNotifier {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun requestPermission(): Boolean = suspendCoroutine { continuation ->
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            continuation.resume(granted)
        }
    }

    override fun schedule(announcements: List<ScheduledAnnouncement>) {
        for (announcement in announcements) {
            val content = UNMutableNotificationContent().apply {
                setTitle(announcement.title)
                setBody(announcement.body)
                setSound(UNNotificationSound.defaultSound)
            }
            // A calendar trigger fires in the device's current time zone, like the Android schedule.
            val fireAt = announcement.fireAt
            val components = NSDateComponents().apply {
                setYear(fireAt.year.toLong())
                setMonth(fireAt.month.number.toLong())
                setDay(fireAt.day.toLong())
                setHour(fireAt.hour.toLong())
                setMinute(fireAt.minute.toLong())
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
            center.addNotificationRequest(
                UNNotificationRequest.requestWithIdentifier(announcement.id, content, trigger),
                withCompletionHandler = null
            )
        }
    }

    override fun cancel(ids: List<String>) {
        center.removePendingNotificationRequestsWithIdentifiers(ids)
    }
}
