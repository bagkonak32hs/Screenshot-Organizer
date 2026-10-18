package com.example.ui.theme

enum class AppThemeMode(
    val title: String,
    val subtitle: String
) {
    SYSTEM(
        title = "Sistem Varsayılanı",
        subtitle = "Cihaz ayarlarına göre otomatik değişir"
    ),
    LIGHT(
        title = "Açık Tema",
        subtitle = "Ferah, net ve canlı açık renkler"
    ),
    DARK(
        title = "Koyu Tema",
        subtitle = "Göz yormayan, premium ve pil dostu mod"
    )
}
