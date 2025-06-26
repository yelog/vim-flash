package org.yelog.ideavim.flash.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications


/**
 * Utility function to show a notification message in the IDE.
 * @param message The message to display in the notification.
 * @param title The title of the notification (default is "vim-flash message").
 */
fun notify(message: String, title: String = "vim-flash message") {
    val notification = NotificationGroupManager.getInstance()
        .getNotificationGroup("org.yelog.ideavim.flash")
        .createNotification(title, message, NotificationType.INFORMATION)

    Notifications.Bus.notify(notification)
}
