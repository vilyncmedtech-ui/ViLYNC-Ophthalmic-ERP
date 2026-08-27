package com.vilync.ophthalmicerp.core.reports.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.vilync.ophthalmicerp.core.reports.domain.FilterType
import com.vilync.ophthalmicerp.core.reports.domain.ReportFilterDescriptor
import kotlinx.coroutines.delay

@Composable
fun SmartFilterBar(
    filters: List<ReportFilterDescriptor>,
    currentValues: Map<String, Any?>,
    dynamicOptions: Map<String, List<String>> = emptyMap(),
    onFilterChange: (String, Any?) -> Unit,
    onApply: () -> Unit
) {
    val groupedFilters = filters.groupBy { it.section ?: "GENERAL" }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        groupedFilters.forEach { (section, sectionFilters) ->
            FilterSection(
                title = section,
                filters = sectionFilters,
                currentValues = currentValues,
                dynamicOptions = dynamicOptions,
                onFilterChange = onFilterChange,
                onApply = onApply
            )
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    filters: List<ReportFilterDescriptor>,
    currentValues: Map<String, Any?>,
    dynamicOptions: Map<String, List<String>>,
    onFilterChange: (String, Any?) -> Unit,
    onApply: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title != "GENERAL") {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF667085),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        var i = 0
        while (i < filters.size) {
            val currentFilter = filters[i]
            
            if (currentFilter.weight < 1f && i + 1 < filters.size && filters[i+1].weight < 1f) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(currentFilter.weight)) {
                        FilterItem(currentFilter, currentValues, dynamicOptions, onFilterChange, onApply)
                    }
                    Box(modifier = Modifier.weight(filters[i+1].weight)) {
                        FilterItem(filters[i+1], currentValues, dynamicOptions, onFilterChange, onApply)
                    }
                }
                i += 2
            } else {
                FilterItem(currentFilter, currentValues, dynamicOptions, onFilterChange, onApply)
                i++
            }
        }
    }
}

@Composable
private fun FilterItem(
    filter: ReportFilterDescriptor,
    currentValues: Map<String, Any?>,
    dynamicOptions: Map<String, List<String>>,
    onFilterChange: (String, Any?) -> Unit,
    onApply: () -> Unit
) {
    when (filter.type) {
        FilterType.SEARCH_BAR -> {
            SearchBarFilter(
                label = filter.label,
                icon = filter.icon,
                value = currentValues[filter.id] as? String ?: "",
                onValueChange = { onFilterChange(filter.id, it) }
            )
        }
        FilterType.DATE_RANGE -> {
            DatePickerFilter(
                label = filter.label,
                icon = filter.icon,
                dateValue = currentValues[filter.id] as? String ?: "",
                onDateChange = { onFilterChange(filter.id, it) }
            )
        }
        FilterType.DROPDOWN -> {
            val options = dynamicOptions[filter.id] ?: filter.options ?: emptyList()
            DropdownFilter(
                label = filter.label,
                icon = filter.icon,
                value = currentValues[filter.id] as? String ?: "",
                options = options,
                enableSearch = filter.enableSearch,
                onValueChange = { onFilterChange(filter.id, it) }
            )
        }
        FilterType.AUTOCOMPLETE -> {
            val options = dynamicOptions[filter.id] ?: emptyList()
            AutocompleteFilter(
                label = filter.label,
                icon = filter.icon,
                value = currentValues[filter.id] as? String ?: "",
                options = options,
                onValueChange = { onFilterChange(filter.id, it) }
            )
        }
        else -> { /* TODO */ }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerFilter(
    label: String,
    icon: ImageVector?,
    dateValue: String,
    onDateChange: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    var showPicker by remember { mutableStateOf(false) }
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        onDateChange(sdf.format(java.util.Date(ms)))
                    }
                    showPicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF475467))
                Spacer(Modifier.width(6.dp))
            }
            Text(text = label, fontSize = 12.sp, color = Color(0xFF344054), fontWeight = FontWeight.Medium)
        }
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF101828)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D5DD)),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(dateValue.ifBlank { "Select Date" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF667085))
        }
    }
}

@Composable
private fun SearchBarFilter(
    label: String,
    icon: ImageVector?,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF475467))
                Spacer(Modifier.width(6.dp))
            }
            Text(text = label, fontSize = 12.sp, color = Color(0xFF344054), fontWeight = FontWeight.Medium)
        }
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                color = Color(0xFF101828)
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFD0D5DD), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = "Enter ${label.lowercase()}...",
                                fontSize = 13.sp,
                                color = Color(0xFF667085)
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun DropdownFilter(
    label: String,
    icon: ImageVector?,
    value: String, // This is the ID (e.g., "2" or "0" or "All Products")
    options: List<String>, // Format: "ID|Name|Subtext" or just "Name"
    enableSearch: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var menuSearchQuery by remember { mutableStateOf("") }
    
    // Clear search query when value is reset or changed from outside
    LaunchedEffect(value) {
        menuSearchQuery = ""
    }

    // Helper to extract Name for display
    val displayLabel = remember(value, options) {
        val found = options.find { it.startsWith("$value|") }
        if (found != null) {
            found.split("|").getOrNull(1) ?: value
        } else {
            // Backward compatibility for simple options or "All Products"
            value
        }
    }

    val filteredOptions = if (!enableSearch || menuSearchQuery.isBlank()) options
    else options.filter { it.contains(menuSearchQuery, ignoreCase = true) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF475467))
                Spacer(Modifier.width(6.dp))
            }
            Text(text = label, fontSize = 12.sp, color = Color(0xFF344054), fontWeight = FontWeight.Medium)
        }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF101828)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D5DD)),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(
                    text = displayLabel.ifBlank { "Select $label" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF667085))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { 
                    expanded = false
                    menuSearchQuery = ""
                },
                modifier = Modifier.width(260.dp)
            ) {
                if (enableSearch) {
                    OutlinedTextField(
                        value = menuSearchQuery,
                        onValueChange = { menuSearchQuery = it },
                        modifier = Modifier.padding(8.dp).fillMaxWidth().height(48.dp),
                        placeholder = { Text("Search...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFD0D5DD)
                        )
                    )
                }

                filteredOptions.forEach { option ->
                    val parts = option.split("|")
                    val id = parts[0]
                    val name = if (parts.size > 1) parts[1] else parts[0]
                    val subtext = if (parts.size > 2) parts[2] else ""

                    DropdownMenuItem(
                        text = { 
                            Column {
                                Text(name, fontSize = 14.sp)
                                if (subtext.isNotBlank()) {
                                    Text(subtext, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        },
                        onClick = {
                            onValueChange(id)
                            expanded = false
                            menuSearchQuery = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AutocompleteFilter(
    label: String,
    icon: ImageVector?,
    value: String, // Internal value (e.g. ID)
    options: List<String>, // Format: "ID|Name|Subtext"
    onValueChange: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    // Parse selected option to display name
    val selectedName = remember(value, options) {
        options.find { it.startsWith("$value|") }?.split("|")?.getOrNull(1) ?: value
    }

    // Reset search query when value is reset (e.g. to "0" or "All Products")
    LaunchedEffect(value) {
        if (value == "0" || value == "All Products" || value.isEmpty()) {
            searchQuery = ""
        }
    }
    
    val filteredOptions = remember(searchQuery, options) {
        if (searchQuery.isBlank()) options.take(20)
        else options.filter { option ->
            val parts = option.split("|")
            val name = parts.getOrNull(1) ?: ""
            val subtext = parts.getOrNull(2) ?: ""
            name.contains(searchQuery, ignoreCase = true) || subtext.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.split("|").getOrNull(1) ?: "" }.take(20)
    }

    val density = LocalDensity.current
    val popupOffset = remember(density) {
        with(density) { 46.dp.roundToPx() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 2.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF475467))
                Spacer(Modifier.width(6.dp))
            }
            Text(text = label, fontSize = 12.sp, color = Color(0xFF344054), fontWeight = FontWeight.Medium)
        }
        
        Box(modifier = Modifier.fillMaxWidth()) {
            val textToDisplay = if (isDropdownExpanded) searchQuery else selectedName
            
            BasicTextField(
                value = textToDisplay,
                onValueChange = { 
                    searchQuery = it
                    isDropdownExpanded = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .onFocusChanged { 
                        if (it.isFocused) isDropdownExpanded = true 
                    },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = Color(0xFF101828)
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFD0D5DD), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (textToDisplay.isEmpty()) {
                                Text(
                                    text = "Search ${label.lowercase()}...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF667085)
                                )
                            }
                            innerTextField()
                        }
                        IconButton(
                            onClick = { isDropdownExpanded = !isDropdownExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                null,
                                tint = Color(0xFF667085)
                            )
                        }
                    }
                }
            )

            if (isDropdownExpanded && filteredOptions.isNotEmpty()) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = { isDropdownExpanded = false },
                    offset = IntOffset(0, popupOffset)
                ) {
                    Card(
                        modifier = Modifier.width(300.dp).heightIn(max = 240.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        LazyColumn {
                            items(filteredOptions) { option ->
                                val parts = option.split("|")
                                val id = parts[0]
                                val name = parts[1]
                                val subtext = parts.getOrNull(2) ?: ""
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val parts = option.split("|")
                                            val id = parts[0]
                                            val name = parts[1]
                                            onValueChange(id)
                                            searchQuery = name
                                            isDropdownExpanded = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    val displayText = if (subtext.isNotBlank()) "$name ($subtext)" else name
                                    Text(displayText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(color = Color(0xFFF2F4F7))
                            }
                        }
                    }
                }
            }
        }
    }
}
