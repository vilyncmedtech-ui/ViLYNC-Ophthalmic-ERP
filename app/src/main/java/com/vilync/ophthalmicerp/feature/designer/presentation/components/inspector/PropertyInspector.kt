package com.vilync.ophthalmicerp.feature.designer.presentation.components.inspector

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vilync.ophthalmicerp.feature.designer.domain.property.PropertyChange
import com.vilync.ophthalmicerp.feature.designer.domain.property.PropertyEditorModel
import com.vilync.ophthalmicerp.feature.designer.domain.property.PropertyValidationResult

@Composable
fun PropertyInspector(
    model: PropertyEditorModel?,
    currentValues: Map<String, Any?>,
    validationResults: Map<String, PropertyValidationResult>,
    onPropertyChange: (PropertyChange) -> Unit,
    onPropertyCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = 320.dp, max = 420.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (model == null) {
            EmptyInspector()
        } else {
            ActiveInspector(
                model = model,
                currentValues = currentValues,
                validationResults = validationResults,
                onPropertyChange = onPropertyChange,
                onPropertyCommit = onPropertyCommit
            )
        }
    }
}

@Composable
private fun ActiveInspector(
    model: PropertyEditorModel,
    currentValues: Map<String, Any?>,
    validationResults: Map<String, PropertyValidationResult>,
    onPropertyChange: (PropertyChange) -> Unit,
    onPropertyCommit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Property Inspector",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Black
        )
        HorizontalDivider()
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            model.groups.forEach { group ->
                item {
                    Text(
                        text = group.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(group.descriptors) { descriptor ->
                    PropertyEditor(
                        descriptor = descriptor,
                        currentValue = currentValues[descriptor.id],
                        validationResult = validationResults[descriptor.id],
                        onValueChange = { newValue ->
                            onPropertyChange(
                                PropertyChange(
                                    objectId = model.objectId,
                                    propertyId = descriptor.id,
                                    newValue = newValue
                                )
                            )
                        },
                        onCommit = onPropertyCommit
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EmptyInspector() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No object selected",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Select an element to edit its properties",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
