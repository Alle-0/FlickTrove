package com.cinetrack.ui.components.shared

import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class ImportState {
    object Select : ImportState()
    object Processing : ImportState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBottomSheet(
    isVisible: Boolean,
    state: ImportState,
    onClose: () -> Unit
) {
    if (isVisible) {
        FlickTroveBottomSheet(onDismissRequest = onClose) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(ImageVector.vectorResource(id = R.drawable.ic_ricarica), null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Importa Biblioteca", color = Color.White, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(24.dp))
                if (state is ImportState.Select) {
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_lista), null)
                        Spacer(Modifier.width(8.dp))
                        Text("Carica CSV")
                    }
                }
            }
        }
    }
}
