package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AccentPreset
import com.example.data.local.AppearanceMode
import com.example.data.local.FontScaleChoice
import com.example.data.local.PersianFontChoice
import com.example.data.local.UserSettings

// Five legitimate bundled open-source Persian fonts from res/font/
val VazirmatnFontFamily = FontFamily(Font(R.font.vazirmatn, FontWeight.Normal))
val PlexArabicFontFamily = FontFamily(Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal))
val NotoArabicFontFamily = FontFamily(Font(R.font.noto_sans_arabic, FontWeight.Normal))
val ReadexProFontFamily = FontFamily(Font(R.font.readex_pro, FontWeight.Normal))
val BalooBhaijaanFontFamily = FontFamily(Font(R.font.baloo_bhaijaan_2, FontWeight.Normal))

fun fontFamilyForChoice(choice: PersianFontChoice): FontFamily = when (choice) {
    PersianFontChoice.VAZIRMATN -> VazirmatnFontFamily
    PersianFontChoice.PLEX_ARABIC -> PlexArabicFontFamily
    PersianFontChoice.NOTO_ARABIC -> NotoArabicFontFamily
    PersianFontChoice.READEX_PRO -> ReadexProFontFamily
    PersianFontChoice.BALOO_BHAIJAAN -> BalooBhaijaanFontFamily
}

// 2027 Premium Palette — Deep Navy, Midnight Blue, Porcelain White, Champagne Gold & Warm Bronze
val DeepNavy = Color(0xFF0F172A)
val MidnightBlue = Color(0xFF162238)
val PorcelainCanvas = Color(0xFFF3F6FA)
val SoftIceCanvas = Color(0xFFEAF0F7)
val ChampagneGold = Color(0xFFC59B43)
val WarmBronze = Color(0xFFB47B44)
val EmeraldSuccess = Color(0xFF0D7A5F)
val CrimsonDanger = Color(0xFFC62828)
val AmberWarning = Color(0xFFC27803)
val SapphireInfo = Color(0xFF1D4ED8)

@Immutable
data class GlassTokens(
    val isDark: Boolean = false,
    val cornerRadius: Dp = 24.dp,
    val cardSurface: Color = Color(0xE8FFFFFF),
    val cardSurfaceElevated: Color = Color(0xF5FFFFFF),
    val cardBorder: Color = Color(0x80FFFFFF),
    val subtleDivider: Color = Color(0x1F0F172A),
    val canvasTop: Color = Color(0xFFF4F7FC),
    val canvasBottom: Color = Color(0xFFE8EFF7),
    val ambientBlobPrimary: Color = Color(0x1A13294B),
    val ambientBlobAccent: Color = Color(0x1EC59B43),
    val accentPrimary: Color = Color(0xFF13294B),
    val accentSecondary: Color = Color(0xFFB88946),
    val inkPrimary: Color = Color(0xFF0B1324),
    val inkSecondary: Color = Color(0xFF42526B),
    val usePersianDigits: Boolean = true,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = true
)

val LocalGlassTokens = staticCompositionLocalOf { GlassTokens() }

fun buildLifePilotTypography(fontChoice: PersianFontChoice, scaleChoice: FontScaleChoice): Typography {
    val family = fontFamilyForChoice(fontChoice)
    val s = scaleChoice.scale
    return Typography(
        displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (28 * s).sp, lineHeight = (38 * s).sp),
        headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (24 * s).sp, lineHeight = (34 * s).sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (20 * s).sp, lineHeight = (30 * s).sp),
        headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (18 * s).sp, lineHeight = (26 * s).sp),
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (17 * s).sp, lineHeight = (25 * s).sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (15 * s).sp, lineHeight = (23 * s).sp),
        titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (13.5f * s).sp, lineHeight = (20 * s).sp),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (15 * s).sp, lineHeight = (24 * s).sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (13.5f * s).sp, lineHeight = (21 * s).sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (12 * s).sp, lineHeight = (18 * s).sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (13.5f * s).sp, lineHeight = (19 * s).sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (12 * s).sp, lineHeight = (17 * s).sp),
        labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (11 * s).sp, lineHeight = (15 * s).sp)
    )
}

@Composable
fun LifePilotTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> systemDark
    }

    val primaryColor = Color(settings.accentPreset.primaryHex)
    val secondaryColor = Color(settings.accentPreset.secondaryHex)
    val effectiveAlpha = if (settings.highContrast) settings.cardAlpha.coerceAtLeast(0.84f) else settings.cardAlpha
    val radius = settings.cornerRadiusDp.coerceIn(14, 32).dp

    val glassTokens = if (!isDark) {
        GlassTokens(
            isDark = false,
            cornerRadius = radius,
            cardSurface = Color.White.copy(alpha = effectiveAlpha),
            cardSurfaceElevated = Color.White.copy(alpha = (effectiveAlpha + 0.08f).coerceAtMost(0.98f)),
            cardBorder = primaryColor.copy(alpha = (settings.borderAlpha * 0.30f).coerceIn(0.10f, 0.28f)),
            subtleDivider = Color(0xFF0F172A).copy(alpha = 0.09f),
            canvasTop = Color(0xFFF5F8FC),
            canvasBottom = Color(0xFFE9EFF8),
            ambientBlobPrimary = primaryColor.copy(alpha = 0.09f * settings.glassIntensity),
            ambientBlobAccent = secondaryColor.copy(alpha = 0.13f * settings.glassIntensity),
            accentPrimary = primaryColor,
            accentSecondary = secondaryColor,
            inkPrimary = Color(0xFF0B1324),
            inkSecondary = if (settings.highContrast) Color(0xFF334155) else Color(0xFF52627A),
            usePersianDigits = settings.usePersianDigits,
            reducedMotion = settings.reducedMotion,
            highContrast = settings.highContrast
        )
    } else {
        val darkCardAlpha = (effectiveAlpha * 0.85f).coerceIn(0.62f, 0.92f)
        GlassTokens(
            isDark = true,
            cornerRadius = radius,
            cardSurface = Color(0xFF152238).copy(alpha = darkCardAlpha),
            cardSurfaceElevated = Color(0xFF1B2B45).copy(alpha = (darkCardAlpha + 0.08f).coerceAtMost(0.96f)),
            cardBorder = Color(0xFFFFFFFF).copy(alpha = (settings.borderAlpha * 0.35f).coerceIn(0.10f, 0.28f)),
            subtleDivider = Color(0xFFFFFFFF).copy(alpha = 0.10f),
            canvasTop = Color(0xFF0B1324),
            canvasBottom = Color(0xFF111D33),
            ambientBlobPrimary = Color(0xFF2563EB).copy(alpha = 0.16f * settings.glassIntensity),
            ambientBlobAccent = secondaryColor.copy(alpha = 0.14f * settings.glassIntensity),
            accentPrimary = Color(0xFF7CB2FF),
            accentSecondary = Color(0xFFE0B96A),
            inkPrimary = Color(0xFFF8FAFC),
            inkSecondary = if (settings.highContrast) Color(0xFFCBD5E1) else Color(0xFF94A3B8),
            usePersianDigits = settings.usePersianDigits,
            reducedMotion = settings.reducedMotion,
            highContrast = settings.highContrast
        )
    }

    val colorScheme = if (!isDark) {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.12f),
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = secondaryColor.copy(alpha = 0.15f),
            onSecondaryContainer = Color(0xFF5A3D14),
            tertiary = EmeraldSuccess,
            background = PorcelainCanvas,
            onBackground = glassTokens.inkPrimary,
            surface = Color.White,
            onSurface = glassTokens.inkPrimary,
            surfaceVariant = SoftIceCanvas,
            onSurfaceVariant = glassTokens.inkSecondary,
            outline = glassTokens.cardBorder,
            error = CrimsonDanger,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = glassTokens.accentPrimary,
            onPrimary = DeepNavy,
            primaryContainer = Color(0xFF1E3A5F),
            onPrimaryContainer = Color(0xFFE2EEFF),
            secondary = glassTokens.accentSecondary,
            onSecondary = DeepNavy,
            secondaryContainer = Color(0xFF3E3018),
            onSecondaryContainer = Color(0xFFFFE4AE),
            tertiary = Color(0xFF34D399),
            background = DeepNavy,
            onBackground = glassTokens.inkPrimary,
            surface = MidnightBlue,
            onSurface = glassTokens.inkPrimary,
            surfaceVariant = Color(0xFF1D2D47),
            onSurfaceVariant = glassTokens.inkSecondary,
            outline = glassTokens.cardBorder,
            error = Color(0xFFF87171),
            onError = DeepNavy
        )
    }

    val shapes = Shapes(
        small = RoundedCornerShape((settings.cornerRadiusDp * 0.55f).toInt().coerceAtLeast(10).dp),
        medium = RoundedCornerShape((settings.cornerRadiusDp * 0.8f).toInt().coerceAtLeast(14).dp),
        large = RoundedCornerShape(radius),
        extraLarge = RoundedCornerShape((settings.cornerRadiusDp + 4).dp)
    )

    val typography = buildLifePilotTypography(settings.fontChoice, settings.fontScale)

    // Enforce native RTL across the entire Compose hierarchy at root!
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalGlassTokens provides glassTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
