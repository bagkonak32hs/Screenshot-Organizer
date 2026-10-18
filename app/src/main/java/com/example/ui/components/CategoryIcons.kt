package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            "Work" -> Icons.Default.Work
            "School" -> Icons.Default.School
            "SportsEsports" -> Icons.Default.SportsEsports
            "Person" -> Icons.Default.Person
            "ReceiptLong" -> Icons.AutoMirrored.Filled.ReceiptLong
            "LocationOn" -> Icons.Default.LocationOn
            "Chat" -> Icons.AutoMirrored.Filled.Chat
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "Receipt" -> Icons.Default.Receipt
            "Description" -> Icons.Default.Description
            "AccountBalance" -> Icons.Default.AccountBalance
            "Flight" -> Icons.Default.Flight
            "EventSeat" -> Icons.Default.EventSeat
            "Phone" -> Icons.Default.Phone
            "StickyNote2" -> Icons.AutoMirrored.Filled.StickyNote2
            "RestaurantMenu" -> Icons.Default.RestaurantMenu
            "Share" -> Icons.Default.Share
            "Lightbulb" -> Icons.Default.Lightbulb
            else -> Icons.Default.Folder
        }
    }

    fun parseColor(hex: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color(0xFF6366F1)
        }
    }
}
