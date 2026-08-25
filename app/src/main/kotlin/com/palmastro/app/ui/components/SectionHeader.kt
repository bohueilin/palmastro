package com.palmastro.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The app's one icon + label section heading. It existed as four private copies that
 * had drifted apart; the copy on Domain Detail also omitted [heading], which silently
 * removed that screen from TalkBack's heading navigation.
 *
 * [lineHeight] is stated explicitly: without it the line box inherits bodyLarge's
 * 24.sp from MaterialTheme's ambient text style, which made the header row 4dp taller
 * on whichever screen forgot it.
 */
@Composable
fun SectionHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) { heading() },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp,
        )
    }
}
