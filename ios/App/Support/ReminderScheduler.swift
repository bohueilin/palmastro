import Foundation
import UserNotifications

/// Schedules the opt-in monthly rescan reminder (PRD §23). Everything is
/// local: a single repeating calendar trigger, no reading or profile content
/// in the payload, removed as soon as the toggle turns off or the user
/// deletes all data.
enum ReminderScheduler {

    /// Stable identifier of the single pending monthly reminder request.
    static let monthlyIdentifier = "palmastro.reminder.monthly"

    /// Requests notification permission if needed, then schedules the
    /// repeating monthly reminder. `completion` runs on the main queue and
    /// receives `false` when permission is denied so the caller can revert
    /// the toggle gracefully.
    static func enable(
        lastScanDate: Date?,
        center: UNUserNotificationCenter = .current(),
        completion: @escaping (Bool) -> Void
    ) {
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            if granted {
                center.add(monthlyRequest(lastScanDate: lastScanDate))
            }
            DispatchQueue.main.async { completion(granted) }
        }
    }

    /// Cancels the pending monthly reminder (toggle off / delete-all-data).
    static func disable(center: UNUserNotificationCenter = .current()) {
        center.removePendingNotificationRequests(withIdentifiers: [monthlyIdentifier])
    }

    /// Day-of-month anchor for the reminder: the last scan's day when known,
    /// otherwise the 1st. Clamped to 28 because a repeating calendar trigger
    /// on day 29–31 would skip shorter months entirely.
    static func anchorDay(lastScanDate: Date?, calendar: Calendar = .current) -> Int {
        guard let lastScanDate else { return 1 }
        return min(calendar.component(.day, from: lastScanDate), 28)
    }

    /// One repeating `UNCalendarNotificationTrigger` at 10:00 local time on
    /// the anchor day. Title/body resolve from Localizable.strings at
    /// delivery time and carry no sensitive content (PRD §23). Re-adding
    /// with the same identifier replaces any previous pending request.
    private static func monthlyRequest(lastScanDate: Date?) -> UNNotificationRequest {
        let content = UNMutableNotificationContent()
        content.title = NSString.localizedUserNotificationString(
            forKey: "reminder_notification_title", arguments: nil
        )
        content.body = NSString.localizedUserNotificationString(
            forKey: "reminder_notification_body", arguments: nil
        )
        content.sound = .default

        var components = DateComponents()
        components.day = anchorDay(lastScanDate: lastScanDate)
        components.hour = 10
        components.minute = 0
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        return UNNotificationRequest(identifier: monthlyIdentifier, content: content, trigger: trigger)
    }
}
