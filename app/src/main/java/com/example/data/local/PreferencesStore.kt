package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lifePilotDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lifepilot_settings"
)

enum class AppearanceMode(val labelFa: String) {
    LIGHT("روشن (گلاسمورفیسم)"),
    DARK("تیره (شبانه)"),
    SYSTEM("هماهنگ با سیستم")
}

enum class PersianFontChoice(
    val displayNameFa: String,
    val subtitleFa: String,
    val previewSample: String
) {
    VAZIRMATN(
        "وزیرمتن (Vazirmatn)",
        "پیش‌فرض، استاندارد و بسیار خوانا",
        "مدیریت هوشمند زندگی ۱۴۰۵ — LifePilot 2027"
    ),
    PLEX_ARABIC(
        "پِلِکس فارسی (جایگزین ایران‌یکان)",
        "هندسی، مدرن و حرفه‌ای",
        "مدیریت هوشمند زندگی ۱۴۰۵ — LifePilot 2027"
    ),
    NOTO_ARABIC(
        "نوتو سنس (جایگزین استعداد)",
        "متعادل، رسمی و شفاف",
        "مدیریت هوشمند زندگی ۱۴۰۵ — LifePilot 2027"
    ),
    READEX_PRO(
        "ریدکس پرو (جایگزین شبنم)",
        "نرم، خوانا و مینیمال",
        "مدیریت هوشمند زندگی ۱۴۰۵ — LifePilot 2027"
    ),
    BALOO_BHAIJAAN(
        "بالو (جایگزین ساحل)",
        "گوشه‌های گرد و صمیمی",
        "مدیریت هوشمند زندگی ۱۴۰۵ — LifePilot 2027"
    )
}

enum class FontScaleChoice(val labelFa: String, val scale: Float) {
    SMALL("کوچک", 0.90f),
    MEDIUM("متوسط", 1.0f),
    LARGE("بزرگ", 1.12f)
}

enum class AccentPreset(
    val labelFa: String,
    val primaryHex: Long,
    val secondaryHex: Long
) {
    NAVY_GOLD("سرمه‌ای و طلایی شامپاینی", 0xFF13294B, 0xFFB88946),
    MIDNIGHT_BRONZE("آبی نیمه‌شب و برنز گرم", 0xFF1E3A5F, 0xFFA96E3C),
    SLATE_SAPPHIRE("یاقوتی و نقره‌ای", 0xFF1D4ED8, 0xFF0F766E),
    DEEP_TEAL_GOLD("فیروزه‌ای تیره و طلایی", 0xFF0F4C5C, 0xFFC59B43)
}

enum class DashboardWidgetId(val titleFa: String) {
    FINANCE("خلاصه مالی و نرخ دلار"),
    AGENDA("برنامه امروز"),
    TASKS("وظایف امروز"),
    INSTALLMENTS("اقساط پیش‌رو و معوق"),
    UNIVERSITY("برنامه دانشگاه"),
    PROJECTS("پیشرفت پروژه‌ها"),
    HABITS("عادت‌های امروز"),
    NOTES("یادداشت‌های اخیر"),
    ACTIVITY("فعالیت‌های اخیر")
}

data class UserSettings(
    val userName: String = "کاربر لایف‌پایلوت",
    val appearanceMode: AppearanceMode = AppearanceMode.LIGHT,
    val fontChoice: PersianFontChoice = PersianFontChoice.VAZIRMATN,
    val fontScale: FontScaleChoice = FontScaleChoice.MEDIUM,
    val accentPreset: AccentPreset = AccentPreset.NAVY_GOLD,
    val glassIntensity: Float = 0.72f,
    val cardAlpha: Float = 0.86f,
    val borderAlpha: Float = 0.45f,
    val cornerRadiusDp: Int = 24,
    val usePersianDigits: Boolean = true,
    val defaultCurrency: String = "IRT",
    val calendarWeekStart: Int = 0, // 0 = Saturday
    val showWeekNumbers: Boolean = true,
    val showOfficialHolidays: Boolean = true,
    val calendarDefaultView: String = "MONTH",
    val highContrast: Boolean = true,
    val reducedMotion: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val notificationSound: Boolean = true,
    val timeZoneId: String = "Asia/Tehran",
    val widgetOrder: List<DashboardWidgetId> = DashboardWidgetId.entries,
    val hiddenWidgets: Set<DashboardWidgetId> = emptySet(),
    val appLockPin: String = ""
)

class PreferencesStore(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val APPEARANCE = stringPreferencesKey("appearance")
        val FONT_CHOICE = stringPreferencesKey("font_choice")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val CARD_ALPHA = floatPreferencesKey("card_alpha")
        val BORDER_ALPHA = floatPreferencesKey("border_alpha")
        val CORNER_RADIUS = intPreferencesKey("corner_radius")
        val PERSIAN_DIGITS = booleanPreferencesKey("persian_digits")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val WEEK_START = intPreferencesKey("week_start")
        val SHOW_WEEK_NUMBERS = booleanPreferencesKey("show_week_numbers")
        val SHOW_HOLIDAYS = booleanPreferencesKey("show_holidays")
        val CALENDAR_VIEW = stringPreferencesKey("calendar_view")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound")
        val TIMEZONE_ID = stringPreferencesKey("timezone_id")
        val WIDGET_ORDER = stringPreferencesKey("widget_order")
        val HIDDEN_WIDGETS = stringPreferencesKey("hidden_widgets")
        val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    }

    val settingsFlow: Flow<UserSettings> = context.lifePilotDataStore.data.map { prefs ->
        val orderStr = prefs[Keys.WIDGET_ORDER]
        val parsedOrder = if (orderStr.isNullOrBlank()) {
            DashboardWidgetId.entries
        } else {
            val found = orderStr.split(",").mapNotNull { name ->
                DashboardWidgetId.entries.find { it.name == name }
            }
            val missing = DashboardWidgetId.entries.filterNot { it in found }
            found + missing
        }

        val hiddenStr = prefs[Keys.HIDDEN_WIDGETS].orEmpty()
        val hiddenSet = hiddenStr.split(",").mapNotNull { name ->
            DashboardWidgetId.entries.find { it.name == name }
        }.toSet()

        UserSettings(
            userName = prefs[Keys.USER_NAME] ?: "کاربر لایف‌پایلوت",
            appearanceMode = enumValueOrDefault(prefs[Keys.APPEARANCE], AppearanceMode.LIGHT),
            fontChoice = enumValueOrDefault(prefs[Keys.FONT_CHOICE], PersianFontChoice.VAZIRMATN),
            fontScale = enumValueOrDefault(prefs[Keys.FONT_SCALE], FontScaleChoice.MEDIUM),
            accentPreset = enumValueOrDefault(prefs[Keys.ACCENT_PRESET], AccentPreset.NAVY_GOLD),
            glassIntensity = prefs[Keys.GLASS_INTENSITY] ?: 0.72f,
            cardAlpha = prefs[Keys.CARD_ALPHA] ?: 0.86f,
            borderAlpha = prefs[Keys.BORDER_ALPHA] ?: 0.45f,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 24,
            usePersianDigits = prefs[Keys.PERSIAN_DIGITS] ?: true,
            defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "IRT",
            calendarWeekStart = prefs[Keys.WEEK_START] ?: 0,
            showWeekNumbers = prefs[Keys.SHOW_WEEK_NUMBERS] ?: true,
            showOfficialHolidays = prefs[Keys.SHOW_HOLIDAYS] ?: true,
            calendarDefaultView = prefs[Keys.CALENDAR_VIEW] ?: "MONTH",
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            notificationSound = prefs[Keys.NOTIFICATION_SOUND] ?: true,
            timeZoneId = prefs[Keys.TIMEZONE_ID] ?: "Asia/Tehran",
            widgetOrder = parsedOrder,
            hiddenWidgets = hiddenSet,
            appLockPin = prefs[Keys.APP_LOCK_PIN] ?: ""
        )
    }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.lifePilotDataStore.edit { prefs ->
            val current = UserSettings(
                userName = prefs[Keys.USER_NAME] ?: "کاربر لایف‌پایلوت",
                appearanceMode = enumValueOrDefault(prefs[Keys.APPEARANCE], AppearanceMode.LIGHT),
                fontChoice = enumValueOrDefault(prefs[Keys.FONT_CHOICE], PersianFontChoice.VAZIRMATN),
                fontScale = enumValueOrDefault(prefs[Keys.FONT_SCALE], FontScaleChoice.MEDIUM),
                accentPreset = enumValueOrDefault(prefs[Keys.ACCENT_PRESET], AccentPreset.NAVY_GOLD),
                glassIntensity = prefs[Keys.GLASS_INTENSITY] ?: 0.72f,
                cardAlpha = prefs[Keys.CARD_ALPHA] ?: 0.86f,
                borderAlpha = prefs[Keys.BORDER_ALPHA] ?: 0.45f,
                cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 24,
                usePersianDigits = prefs[Keys.PERSIAN_DIGITS] ?: true,
                defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "IRT",
                calendarWeekStart = prefs[Keys.WEEK_START] ?: 0,
                showWeekNumbers = prefs[Keys.SHOW_WEEK_NUMBERS] ?: true,
                showOfficialHolidays = prefs[Keys.SHOW_HOLIDAYS] ?: true,
                calendarDefaultView = prefs[Keys.CALENDAR_VIEW] ?: "MONTH",
                highContrast = prefs[Keys.HIGH_CONTRAST] ?: true,
                reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                notificationSound = prefs[Keys.NOTIFICATION_SOUND] ?: true,
                timeZoneId = prefs[Keys.TIMEZONE_ID] ?: "Asia/Tehran",
                widgetOrder = prefs[Keys.WIDGET_ORDER]?.split(",")?.mapNotNull { n ->
                    DashboardWidgetId.entries.find { it.name == n }
                }?.takeIf { it.isNotEmpty() } ?: DashboardWidgetId.entries,
                hiddenWidgets = prefs[Keys.HIDDEN_WIDGETS].orEmpty().split(",").mapNotNull { n ->
                    DashboardWidgetId.entries.find { it.name == n }
                }.toSet(),
                appLockPin = prefs[Keys.APP_LOCK_PIN] ?: ""
            )
            val next = transform(current)
            prefs[Keys.USER_NAME] = next.userName
            prefs[Keys.APPEARANCE] = next.appearanceMode.name
            prefs[Keys.FONT_CHOICE] = next.fontChoice.name
            prefs[Keys.FONT_SCALE] = next.fontScale.name
            prefs[Keys.ACCENT_PRESET] = next.accentPreset.name
            prefs[Keys.GLASS_INTENSITY] = next.glassIntensity
            prefs[Keys.CARD_ALPHA] = next.cardAlpha
            prefs[Keys.BORDER_ALPHA] = next.borderAlpha
            prefs[Keys.CORNER_RADIUS] = next.cornerRadiusDp
            prefs[Keys.PERSIAN_DIGITS] = next.usePersianDigits
            prefs[Keys.DEFAULT_CURRENCY] = next.defaultCurrency
            prefs[Keys.WEEK_START] = next.calendarWeekStart
            prefs[Keys.SHOW_WEEK_NUMBERS] = next.showWeekNumbers
            prefs[Keys.SHOW_HOLIDAYS] = next.showOfficialHolidays
            prefs[Keys.CALENDAR_VIEW] = next.calendarDefaultView
            prefs[Keys.HIGH_CONTRAST] = next.highContrast
            prefs[Keys.REDUCED_MOTION] = next.reducedMotion
            prefs[Keys.NOTIFICATIONS_ENABLED] = next.notificationsEnabled
            prefs[Keys.NOTIFICATION_SOUND] = next.notificationSound
            prefs[Keys.TIMEZONE_ID] = next.timeZoneId
            prefs[Keys.WIDGET_ORDER] = next.widgetOrder.joinToString(",") { it.name }
            prefs[Keys.HIDDEN_WIDGETS] = next.hiddenWidgets.joinToString(",") { it.name }
            prefs[Keys.APP_LOCK_PIN] = next.appLockPin
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, default: T): T {
        if (name == null) return default
        return enumValues<T>().find { it.name == name } ?: default
    }
}
