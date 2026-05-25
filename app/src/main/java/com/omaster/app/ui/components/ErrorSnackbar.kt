package com.omaster.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(16.dp)
    ) { data ->
        Snackbar(
            action = {
                if (data.actionLabel != null) {
                    TextButton(onClick = { snackbarHostState.currentSnackbarData?.dismiss() }) {
                        Text(data.actionLabel)
                    }
                }
            }
        ) {
            Text(data.visuals.message)
        }
    }
}

@Composable
fun InfoSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(16.dp)
    ) { data ->
        Snackbar {
            Text(data.visuals.message)
        }
    }
}