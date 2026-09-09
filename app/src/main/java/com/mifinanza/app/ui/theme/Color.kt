package com.mifinanza.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Monet brand palette ───────────────────────────────────────────────────────
val GreenBrand    = Color(0xFF00C6A0)   // Monet teal — primary CTA
val RedBrand      = Color(0xFFEF4444)   // expense / danger
val AmberBrand    = Color(0xFFF59E0B)   // pending / warning
val BlueBrand     = Color(0xFF3B82F6)   // MSI / info
val PurpleBrand   = Color(0xFF8B5CF6)   // subscriptions

// ── Dark theme ────────────────────────────────────────────────────────────────
val BgDark        = Color(0xFF0A0F1A)   // page background
val Surface       = Color(0xFF131B2A)   // card
val SurfaceVar    = Color(0xFF1C2638)   // elevated card
val BorderColor   = Color(0xFF2D3A50)   // border

val TextPrimary   = Color(0xFFF0F4FF)
val TextSecondary = Color(0xFF8B95A8)
val TextTertiary  = Color(0xFF4A5568)

// Category colors
fun categoryColor(id: String): Color = when (id) {
    "food"          -> Color(0xFFF97316)
    "transport"     -> Color(0xFF60A5FA)
    "entertainment" -> Color(0xFFA78BFA)
    "health"        -> Color(0xFFEF4444)
    "clothing"      -> Color(0xFFF472B6)
    "tech"          -> Color(0xFF22D3EE)
    "home"          -> Color(0xFF86EFAC)
    "subscriptions" -> Color(0xFF818CF8)
    "salary"        -> GreenBrand
    else            -> Color(0xFF94A3B8)
}
