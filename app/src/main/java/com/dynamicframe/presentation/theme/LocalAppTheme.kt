package com.dynamicframe.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.dynamicframe.domain.model.AppTheme

val LocalAppTheme = staticCompositionLocalOf { AppTheme.DEFAULT }

@Composable
fun ProvideAppTheme(theme: AppTheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppTheme provides theme, content = content)
}

fun AppTheme.isParadise(): Boolean = this == AppTheme.PARADISE
