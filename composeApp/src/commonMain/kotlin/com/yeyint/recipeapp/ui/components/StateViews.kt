package com.yeyint.recipeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.yeyint.recipeapp.shared.util.AppError
import com.yeyint.recipeapp.shared.util.displayMessage
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Loading / empty / error building blocks (EmptyViewPod equivalents). */

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EmptyView(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ErrorView(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            error.displayMessage(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Preview
@Composable
private fun LoadingViewPreview() {
    MaterialTheme {
        Surface {
            LoadingView()
        }
    }
}

@Preview
@Composable
private fun EmptyViewPreview() {
    MaterialTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EmptyView(title = "No recipes yet")
                EmptyView(
                    title = "Nothing found",
                    subtitle = "Try adjusting your search or filters.",
                )
            }
        }
    }
}

@Preview
@Composable
private fun ErrorViewPreview() {
    MaterialTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ErrorView(error = AppError.Network, onRetry = {})
                ErrorView(error = AppError.Timeout)
                ErrorView(error = AppError.Unknown("Something went wrong"))
            }
        }
    }
}
