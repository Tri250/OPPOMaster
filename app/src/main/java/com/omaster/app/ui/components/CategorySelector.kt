package com.omaster.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omaster.app.model.*
import com.omaster.app.ui.theme.*

@Composable
fun CategoryTabs(
    categories: List<CategoryItem>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            CategoryChip(
                category = category,
                isSelected = selectedCategory == category.id,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
fun CategoryChip(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    OppoOrange
                } else {
                    SurfaceElevated
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (category.icon != null) {
                Text(category.icon, fontSize = 16.sp)
            }
            Text(
                "${category.name}${if (category.count > 0) " (${category.count})" else ""}",
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) DeepSpace else TextSecondary
            )
        }
    }
}

@Composable
fun SortSelector(
    currentSort: SortType,
    onSortChanged: (SortType) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.width(IntrinsicSize.Max)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated)
                .clickable { isExpanded = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                currentSort.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = TextSecondary
            )
        }
        
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            SortType.values().forEach { sort ->
                DropdownMenuItem(
                    onClick = {
                        onSortChanged(sort)
                        isExpanded = false
                    },
                    text = {
                        Text(
                            sort.displayName,
                            fontSize = 14.sp,
                            color = if (sort == currentSort) OppoOrange else TextPrimary
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun FilterBottomSheet(
    styleCategories: List<CategoryItem>,
    sceneCategories: List<CategoryItem>,
    deviceCategories: List<CategoryItem>,
    selectedStyles: List<String>,
    selectedScenes: List<String>,
    selectedDevices: List<String>,
    onStyleSelected: (String) -> Unit,
    onSceneSelected: (String) -> Unit,
    onDeviceSelected: (String) -> Unit,
    onClearAll: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "筛选",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = onClearAll) {
                    Text("清除全部", color = OppoOrange, fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 风格分类
            FilterSection(
                title = "风格",
                categories = styleCategories,
                selectedItems = selectedStyles,
                onItemSelected = onStyleSelected
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 场景分类
            FilterSection(
                title = "场景",
                categories = sceneCategories,
                selectedItems = selectedScenes,
                onItemSelected = onSceneSelected
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 设备分类
            FilterSection(
                title = "适配机型",
                categories = deviceCategories,
                selectedItems = selectedDevices,
                onItemSelected = onDeviceSelected
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OppoOrange)
            ) {
                Text("应用筛选", color = DeepSpace, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun FilterSection(
    title: String,
    categories: List<CategoryItem>,
    selectedItems: List<String>,
    onItemSelected: (String) -> Unit
) {
    Column {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    category = category,
                    isSelected = selectedItems.contains(category.id),
                    onClick = { onItemSelected(category.id) }
                )
            }
        }
    }
}

@Composable
fun FilterChip(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    OppoOrange
                } else {
                    Surface
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else BorderSubtle,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            category.name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) DeepSpace else TextSecondary
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onVoiceSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary)
        Spacer(modifier = Modifier.width(12.dp))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("搜索预设、风格、作者...", color = TextTertiary) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                containerColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(onClick = onVoiceSearch) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = TextTertiary)
        }
    }
}

@Composable
fun CategoryTypeSelector(
    types: List<CategoryType>,
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        types.forEach { type ->
            TypeTab(
                type = type,
                isSelected = selectedType == type.id,
                onClick = { onTypeSelected(type.id) }
            )
        }
    }
}

@Composable
fun TypeTab(
    type: CategoryType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(type.icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            type.name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) OppoOrange else TextSecondary
        )
    }
}
