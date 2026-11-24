package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.domain.model.AppCategory
import com.example.domain.model.WidgetType

@Composable
fun localizedCategoryName(category: AppCategory): String {
    return when (category) {
        AppCategory.ALL -> stringResource(R.string.category_all)
        AppCategory.PRODUCTIVITY -> stringResource(R.string.category_productivity)
        AppCategory.MEDIA -> stringResource(R.string.category_media)
        AppCategory.COMMUNICATION -> stringResource(R.string.category_communication)
        AppCategory.UTILITIES -> stringResource(R.string.category_utilities)
        AppCategory.SYSTEM -> stringResource(R.string.category_system)
    }
}

@Composable
fun localizedWidgetName(type: WidgetType): String {
    return when (type) {
        WidgetType.CLOCK -> stringResource(R.string.widget_clock_title)
        WidgetType.SYSTEM_STATUS -> stringResource(R.string.widget_system_title)
        WidgetType.QUICK_NOTES -> stringResource(R.string.widget_notes_title)
    }
}
