package com.vilync.ophthalmicerp.feature.designer.presentation.components.inspector

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vilync.ophthalmicerp.feature.designer.domain.property.PropertyDescriptor
import com.vilync.ophthalmicerp.feature.designer.domain.property.PropertyType
import com.vilync.ophthalmicerp.feature.designer.domain.property.PropertyValidationResult

@Composable
fun PropertyEditor(
    descriptor: PropertyDescriptor,
    currentValue: Any?,
    onValueChange: (Any?) -> Unit,
    onCommit: () -> Unit,
    validationResult: PropertyValidationResult? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = descriptor.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))

        when (descriptor.type) {
            PropertyType.STRING -> StringEditor(
                value = currentValue?.toString() ?: "",
                onValueChange = onValueChange,
                onCommit = onCommit,
                isError = validationResult?.isValid == false
            )
            PropertyType.NUMBER -> NumericEditor(
                value = currentValue?.toString() ?: "0",
                onValueChange = { onValueChange(it.toDoubleOrNull() ?: 0.0) },
                onCommit = onCommit,
                isError = validationResult?.isValid == false
            )
            PropertyType.BOOLEAN -> BooleanEditor(
                value = currentValue as? Boolean ?: false,
                onValueChange = { 
                    onValueChange(it)
                    onCommit()
                }
            )
            PropertyType.ENUM -> EnumEditor(
                value = currentValue?.toString() ?: "",
                options = descriptor.constraints["options"] as? List<String> ?: emptyList(),
                onValueChange = {
                    onValueChange(it)
                    onCommit()
                }
            )
            PropertyType.COLOR -> ColorEditorPlaceholder()
        }

        if (validationResult?.isValid == false) {
            Text(
                text = validationResult.message ?: "Invalid value",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun StringEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    isError: Boolean
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = {
                onCommit()
                focusManager.clearFocus()
            }
        )
    )
}

@Composable
private fun NumericEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    isError: Boolean
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        textStyle = MaterialTheme.typography.bodyMedium,
        suffix = { Text("mm") },
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = {
                onCommit()
                focusManager.clearFocus()
            }
        )
    )
}

@Composable
private fun BooleanEditor(
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Switch(
        checked = value,
        onCheckedChange = onValueChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnumEditor(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorEditorPlaceholder() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp)
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {}
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "Color Selector (Sprint 22)", style = MaterialTheme.typography.bodySmall)
    }
}
