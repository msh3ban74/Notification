package com.notification.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * A single, consistent "are you sure?" gate for every destructive delete in
 * Rafeeq. Deleting a debt, an installment, a reminder or a note is
 * irreversible, so nothing gets removed on a single tap — the user always
 * confirms first. Bilingual, and the confirm button is tinted with the error
 * color so its weight matches its consequence.
 *
 * Usage: keep a nullable "pending delete" state; set it on the delete tap,
 * render this dialog when it's non-null, and do the real deletion in
 * onConfirm.
 */
@Composable
fun ConfirmDeleteDialog(
    isArabic: Boolean,
    itemLabel: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isArabic) "تأكيد الحذف" else "Confirm delete")
        },
        text = {
            val name = itemLabel?.takeIf { it.isNotBlank() }
            Text(
                if (isArabic) {
                    if (name != null) "متأكد إنك عايز تمسح \"$name\"؟ مش هينفع ترجعه بعد كده."
                    else "متأكد إنك عايز تمسح ده؟ مش هينفع ترجعه بعد كده."
                } else {
                    if (name != null) "Delete \"$name\"? This can't be undone."
                    else "Delete this? This can't be undone."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(
                    text = if (isArabic) "امسح" else "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}
