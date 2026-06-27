package com.acadex.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Colors (New Accent Colors)
val Primary = Color(0xFF6D5DFC)       // Primary Accent (Soft Purple)
val Secondary = Color(0xFF8B7CFF)     // Secondary Accent (Lighter Purple)
val Accent = Color(0xFF8B7CFF)        // Tertiary Accent (Lighter Purple)

// Original Brand Colors specifically for Splash & Auth background gradient
val OriginalPrimary = Color(0xFF4F46E5) // Indigo
val OriginalSecondary = Color(0xFF8B5CF6) // Violet
val OriginalAccent = Color(0xFF38BDF8) // Sky Blue
val BrandGradient = listOf(OriginalPrimary, OriginalSecondary, OriginalAccent)

// Dark Theme Colors
val DarkBackground = Color(0xFF080B14)
val DarkSurface = Color(0xFF151D2E)
val DarkSurfaceVariant = Color(0xFF101827)
val DarkPrimaryText = Color(0xFFFFFFFF)
val DarkSecondaryText = Color(0xFFA3ADC2)
val DarkBorder = Color(0xFF242F45)

// Helper Colors
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)

// Glassmorphism translucent whites/blacks
val GlassWhite = Color(0x33FFFFFF)
val GlassWhiteBorder = Color(0x66FFFFFF)
val GlassBlack = Color(0x33000000)
val GlassBlackBorder = Color(0x44000000)
