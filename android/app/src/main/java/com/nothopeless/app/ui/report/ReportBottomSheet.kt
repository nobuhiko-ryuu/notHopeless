package com.nothopeless.app.ui.report

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nothopeless.app.data.model.ReportReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBottomSheet(
    postId: String,
    onDismiss: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(postId) { viewModel.init(postId) }
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("通報しました。ご協力ありがとうございます")
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("通報理由を選んでください", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            ReportReason.values().forEach { reason ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    RadioButton(
                        selected = uiState.selectedReason == reason,
                        onClick = { viewModel.selectReason(reason) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(reason.label, modifier = Modifier.padding(top = 12.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = { viewModel.onCommentChanged(it) },
                label = { Text("コメント（任意）") },
                supportingText = { Text("${uiState.comment.length}/200") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.submit() },
                enabled = uiState.selectedReason != null && !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("通報する")
            }
            if (uiState.hasError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "通報の送信に失敗しました",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
