package com.example.goodmail.ui.screens.inbox.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.goodmail.ui.screens.inbox.InboxFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    selected: InboxFilter,
    onSelect: (InboxFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == InboxFilter.ALL,
            onClick = { onSelect(InboxFilter.ALL) },
            label = { Text("All") },
        )
        FilterChip(
            selected = selected == InboxFilter.IMPORTANT,
            onClick = { onSelect(InboxFilter.IMPORTANT) },
            label = { Text("Important") },
        )
        FilterChip(
            selected = selected == InboxFilter.OTHERS,
            onClick = { onSelect(InboxFilter.OTHERS) },
            label = { Text("Others") },
        )
    }
}
