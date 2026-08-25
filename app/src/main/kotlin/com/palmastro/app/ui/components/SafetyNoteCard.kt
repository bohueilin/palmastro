package com.palmastro.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The shared "this is reflection, not advice" notice (PRD 13.5). Results, Guidance and
 * Explainability each hand-built it, so the app's trust surface drifted in radius, tint,
 * icon size and type size between screens — the one element that must read as identical
 * everywhere. Callers still supply their own strings: Explainability carries a different,
 * longer honesty statement and deliberately shows no [title].
 *
 * [extra] renders between title and body for the Guidance footer's reflection line.
 * Body type stays at a literal 12sp/18sp rather than bodySmall (13sp/19sp) so adopting
 * the shared component does not silently enlarge the disclaimer.
 */
@Composable
fun SafetyNoteCard(
    body: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    extra: String? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Shield, contentDescription = null,
                modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                if (title != null) {
                    Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                }
                if (extra != null) {
                    Text(
                        extra,
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    body,
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
                )
            }
        }
    }
}
