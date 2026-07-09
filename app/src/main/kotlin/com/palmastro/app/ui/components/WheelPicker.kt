package com.palmastro.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 40.dp,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val halfVisible = visibleCount / 2

    // Only scroll on FIRST composition
    var hasInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasInitialized) {
            listState.scrollToItem(maxOf(0, selectedIndex - halfVisible))
            hasInitialized = true
        }
    }

    Box(modifier = modifier.height(itemHeight * visibleCount)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top padding items
            items(halfVisible) {
                Spacer(Modifier.height(itemHeight))
            }
            items(items.size) { index ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .then(
                            if (isSelected) Modifier else Modifier.clickable {
                                onSelectedChange(index)
                                scope.launch { listState.animateScrollToItem(maxOf(0, index - halfVisible)) }
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(items[index], fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Text(items[index], fontSize = 15.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Bottom padding items
            items(halfVisible) {
                Spacer(Modifier.height(itemHeight))
            }
        }
    }
}
